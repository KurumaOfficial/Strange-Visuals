#version 330

layout(std140) uniform Uniforms {
    vec4 uScreen;
    vec4 uParams;
    vec4 uTint;
    vec4 uTrail;
    vec4 uAnchors;
    vec4 uPick;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 uv;
out vec4 fragColor;

const vec2 SPIRAL[16] = vec2[](
    vec2( 0.000,  1.000), vec2( 0.383,  0.924), vec2( 0.707,  0.707), vec2( 0.924,  0.383),
    vec2( 1.000,  0.000), vec2( 0.924, -0.383), vec2( 0.707, -0.707), vec2( 0.383, -0.924),
    vec2( 0.000, -1.000), vec2(-0.383, -0.924), vec2(-0.707, -0.707), vec2(-0.924, -0.383),
    vec2(-1.000,  0.000), vec2(-0.924,  0.383), vec2(-0.707,  0.707), vec2(-0.383,  0.924)
);

float silhouette(vec2 at) {
    return texture(Sampler1, at).g;
}

float ring(vec2 texel, float radius) {
    float found = 0.0;
    for (int i = 0; i < 16; i++) {
        found = max(found, silhouette(uv + SPIRAL[i] * radius * texel));
    }
    return found;
}

float field(vec2 texel, float reach) {
    float sum = 0.0;
    float weight = 0.0;
    for (int step = 1; step <= 5; step++) {
        float radius = reach * float(step) * 0.2;
        float falloff = 1.0 - float(step - 1) * 0.19;
        float found = 0.0;

        for (int i = 0; i < 16; i++) {
            int index = (i + step * 3) % 16;
            float jitter = 1.0 + float((i + step) % 3) * 0.06;
            found = max(found, silhouette(uv + SPIRAL[index] * radius * jitter * texel));
        }
        sum += found * falloff;
        weight += falloff;
    }
    return sum / max(weight, 0.0001);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 3; i++) {
        sum += noise(p) * amp;
        p *= 2.07;
        amp *= 0.5;
    }
    return sum;
}

void main() {
    vec3 scene = texture(Sampler0, uv).rgb;
    vec2 texel = 1.0 / max(uScreen.xy, vec2(1.0));
    float time = uScreen.z;
    int style = int(uScreen.w + 0.5);
    float width = uParams.x;
    float strength = uParams.y;
    float trailStrength = uParams.z;
    float reach = uParams.w;

    vec4 buffered = texture(Sampler1, uv);
    float inside = buffered.g;
    float outside = 1.0 - inside;
    vec3 result = scene;

    float trail = max(0.0, buffered.r - inside);
    if (trailStrength > 0.0 && trail > 0.003) {
        float shaped = clamp(pow(trail, 0.75) * trailStrength, 0.0, 1.0);

        float grain = 0.45 + 0.55 * fbm(uv * vec2(18.0, 11.0) + vec2(time * 0.2, -time * 0.75));
        vec3 hot = mix(uTrail.rgb, vec3(1.0), 0.18);
        vec3 cold = uTrail.rgb * 0.42;
        vec3 tone = mix(cold, hot, trail);
        result = mix(result, tone, clamp(shaped * grain * 0.9, 0.0, 0.92));

        result += tone * pow(trail, 4.0) * trailStrength * 0.5;
    }

    float rim = style > 0 && strength > 0.0 ? ring(texel, width) * outside : 0.0;
    vec3 core = mix(uTint.rgb, vec3(1.0), 0.3);
    vec3 halo = uTint.rgb * 0.8;

    if (style == 1) {
        result += core * rim * strength;
    } else if (style == 2) {
        float soft = field(texel, reach) * outside;
        result += halo * soft * soft * strength * 0.9;
        result += core * rim * strength;
    } else if (style == 3) {
        float soft = field(texel, reach * 1.6) * outside;
        float breath = 0.72 + 0.28 * sin(time * 1.6);
        result += halo * soft * soft * strength * 1.5 * breath;
        result += core * rim * strength * 0.35;
    } else if (style == 4) {
        float flow = noise(uv * vec2(26.0, 18.0) + vec2(time * 0.9, -time * 1.7));
        float crackle = 0.45 + 0.85 * flow;
        float r = ring(texel, width * 0.82) * outside;
        float g = rim;
        float b = ring(texel, width * 1.2) * outside;
        vec3 split = vec3(r, g, b) * core;
        float soft = field(texel, reach) * outside;
        result += halo * soft * soft * strength * 0.7;
        result += split * crackle * strength * 1.25;
    }

    float held = uPick.x;
    float over = uPick.y;
    if (held > 0.5 || over > 0.5) {
        float toRight = distance(uv, uAnchors.xy);
        float toLeft = distance(uv, uAnchors.zw);
        float mine = toRight < toLeft ? 1.0 : 2.0;

        float edge = max(ring(texel, width * 1.15), ring(texel, width * 2.1) * 0.55) * outside;
        if (abs(mine - held) < 0.5) {
            float pulse = 0.86 + 0.14 * sin(time * 5.0);
            result += vec3(1.0) * edge * 1.5 * pulse;
            result += vec3(1.0) * field(texel, reach * 0.7) * outside * 0.35;
        } else if (abs(mine - over) < 0.5) {
            result += vec3(1.0) * edge * 0.55;
        }
    }

    fragColor = vec4(result, 1.0);
}
