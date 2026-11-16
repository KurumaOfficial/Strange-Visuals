#version 330

layout(std140) uniform Uniforms {
    vec4 uScreen;
    vec4 uCamera;
    vec4 uTint;
    vec4 uStep;
    mat4 uInverse;
    mat4 uForward;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 uv;
out vec4 fragColor;

vec3 relativeAt(vec2 at, float depth) {
    vec4 clip = vec4(at * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 point = uInverse * clip;
    return point.xyz / point.w;
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(cell), hash(cell + vec2(1.0, 0.0)), f.x),
               mix(hash(cell + vec2(0.0, 1.0)), hash(cell + vec2(1.0, 1.0)), f.x), f.y);
}

float pooling(vec2 ground) {
    float sum = 0.0;
    float amp = 0.5;
    vec2 p = ground * 0.09;
    for (int i = 0; i < 3; i++) {
        sum += noise(p) * amp;
        p *= 2.11;
        amp *= 0.5;
    }
    return smoothstep(0.3, 0.56, sum);
}

float looksLikeWater(vec3 colour) {
    float bias = colour.b - max(colour.r, colour.g);
    return smoothstep(0.015, 0.1, bias);
}

vec3 ripples(vec2 ground, float time, int layers) {
    vec3 total = vec3(0.0);
    for (int layer = 0; layer < layers; layer++) {
        float scale = 0.32 + float(layer) * 0.26;
        vec2 cell = ground * scale;
        vec2 id = floor(cell);
        vec2 local = fract(cell) - 0.5;

        float seed = hash(id + float(layer) * 17.3);

        float beat = time * (0.4 + seed * 0.3) + seed * 13.0;
        float age = fract(beat);
        vec2 spot = (vec2(hash(id + 3.1), hash(id + 7.7)) - 0.5) * 0.7;
        vec2 offset = local - spot;
        float reach = length(offset) + 0.0001;
        float distance = reach * 2.0;

        vec2 outward = offset / reach * 2.0 * scale;

        float front = age * 1.25;
        float behind = distance - front;
        float carrier = sin(behind * 40.0);
        float envelope = exp(-abs(behind) * 9.0);
        float inside = step(distance, front);

        float life = (1.0 - age) * (1.0 - age) * (1.0 - age);
        float spend = 1.0 - smoothstep(0.0, 1.25, front);
        float weight = life * spend * inside / (0.6 + scale);
        total.x += carrier * envelope * weight;

        float slope = (40.0 * cos(behind * 40.0) - 9.0 * sign(behind) * carrier) * envelope;
        total.yz += outward * slope * weight;

        total.x += smoothstep(0.09, 0.0, distance) * pow(1.0 - age, 8.0) * 1.4;
    }
    return total;
}

float footfall(vec2 ground, float time) {
    float age = uStep.z;
    if (age >= 1.0) {
        return 0.0;
    }
    float distance = length(ground - uStep.xy);
    float front = age * 1.5;
    float behind = distance - front;

    float train = sin(behind * 22.0) * exp(-abs(behind) * 5.0) * step(distance, front);
    float life = (1.0 - age) * (1.0 - age);
    return train * life * 1.3 + smoothstep(0.25, 0.0, distance) * pow(1.0 - age, 5.0);
}

vec3 mirror(vec3 from, vec3 normal, out float found) {
    found = 0.0;
    vec3 ray = reflect(normalize(from), normal);
    if (ray.y < 0.02) {
        return vec3(0.0);
    }

    vec3 at = from;
    float stride = 0.45;

    int steps = uStep.w < 0.5 ? 8 : 14;
    for (int i = 0; i < steps; i++) {
        at += ray * stride;
        stride *= 1.35;
        vec4 clip = uForward * vec4(at, 1.0);
        if (clip.w <= 0.0) {
            return vec3(0.0);
        }
        vec3 ndc = clip.xyz / clip.w;
        if (abs(ndc.x) > 1.0 || abs(ndc.y) > 1.0) {
            return vec3(0.0);
        }
        vec2 screen = ndc.xy * 0.5 + 0.5;
        float sceneDepth = texture(Sampler1, screen).r;
        float rayDepth = ndc.z * 0.5 + 0.5;
        if (rayDepth > sceneDepth + 0.000004) {
            vec2 edge = abs(ndc.xy);
            found = (1.0 - smoothstep(0.72, 1.0, max(edge.x, edge.y))) * (1.0 - float(i) / 18.0);
            return texture(Sampler0, screen).rgb;
        }
    }
    return vec3(0.0);
}

void main() {
    vec3 scene = texture(Sampler0, uv).rgb;
    float depth = texture(Sampler1, uv).r;
    float strength = uScreen.w;
    if (depth > 0.9999) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    float time = uScreen.z;
    vec2 texel = 1.0 / max(uScreen.xy, vec2(1.0));
    vec3 here = relativeAt(uv, depth);

    vec3 alongX = relativeAt(uv + vec2(texel.x, 0.0), texture(Sampler1, uv + vec2(texel.x, 0.0)).r) - here;
    vec3 alongY = relativeAt(uv + vec2(0.0, texel.y), texture(Sampler1, uv + vec2(0.0, texel.y)).r) - here;
    vec3 normal = normalize(cross(alongY, alongX));
    if (normal.y < 0.0) {
        normal = -normal;
    }
    float level = smoothstep(0.6, 0.93, normal.y);
    float away = clamp(length(here) / 40.0, 0.0, 1.0);
    float wetness = level * (1.0 - away * away) * strength;
    if (wetness < 0.01) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    vec2 ground = here.xz + uCamera.xz;

    float open = looksLikeWater(scene) * level;
    float pool = max(pooling(ground), open);
    float water = wetness * mix(0.22, 1.0, pool);

    int layers = uStep.w < 0.5 ? 1 : (away < 0.2 ? 3 : away < 0.5 ? 2 : 1);

    vec3 wave = ripples(ground, time, layers) * mix(0.25, 1.0, pool) * (1.0 + open * 1.6);
    float rings = wave.x + footfall(ground, time);

    vec3 tilted = normalize(normal + vec3(wave.y, 0.0, wave.z) * mix(0.05, 0.11, open));

    vec3 result = mix(scene, scene * 0.55, water * 0.85 * (1.0 - open));

    float grazingTest = 1.0 - abs(dot(normalize(here), tilted));
    float found = 0.0;
    vec3 reflected = vec3(0.0);

    if (water * mix(0.16, 0.96, pow(clamp(grazingTest, 0.0, 1.0), 1.7)) > 0.035) {
        reflected = mirror(here, tilted, found);
    }

    float grazing = 1.0 - abs(dot(normalize(here), tilted));

    float amount = mix(mix(0.16, 0.45, open), 0.96, pow(clamp(grazing, 0.0, 1.0), 1.7));
    if (found > 0.0) {
        result = mix(result, reflected, amount * found * water);
    } else {
        result = mix(result, scene * 1.25 + 0.04, amount * water * 0.35);
    }

    float crest = max(rings, 0.0);
    result += uTint.rgb * crest * water * 0.55;
    result += vec3(1.0) * pow(crest, 3.0) * water * 0.35;
    fragColor = vec4(result, 1.0);
}
