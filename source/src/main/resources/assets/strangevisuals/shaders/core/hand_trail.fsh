#version 330

layout(std140) uniform Uniforms {
    vec4 uScreen;
    vec4 uDrift;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

in vec2 uv;
out vec4 fragColor;

float sample(vec2 at) {
    return step(0.000002, texture(Sampler0, at).r - texture(Sampler1, at).r);
}

float handMask(vec2 at, vec2 texel) {
    vec2 off = texel * 0.25;
    float hit = 0.0;
    hit += sample(at + vec2(off.x, off.y));
    hit += sample(at + vec2(-off.x, off.y));
    hit += sample(at + vec2(off.x, -off.y));
    hit += sample(at + vec2(-off.x, -off.y));
    return hit * 0.25;
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

void main() {
    vec2 texel = 1.0 / max(uScreen.xy, vec2(1.0));
    float decay = uScreen.z;
    vec2 spread = texel * uScreen.w;
    float rise = uDrift.x;
    float wander = uDrift.y;
    float time = uDrift.z;

    vec2 source = uv - vec2(0.0, rise) * texel;
    if (wander > 0.0) {
        float swirl = noise(uv * vec2(9.0, 5.0) + vec2(0.0, -time * 0.55)) - 0.5;
        source.x += swirl * wander * texel.x;
    }

    float held = texture(Sampler2, source).r * 0.2;
    held += texture(Sampler2, source + vec2(spread.x, 0.0)).r * 0.125;
    held += texture(Sampler2, source - vec2(spread.x, 0.0)).r * 0.125;
    held += texture(Sampler2, source + vec2(0.0, spread.y)).r * 0.125;
    held += texture(Sampler2, source - vec2(0.0, spread.y)).r * 0.125;
    held += texture(Sampler2, source + spread * 0.7).r * 0.075;
    held += texture(Sampler2, source - spread * 0.7).r * 0.075;
    held += texture(Sampler2, source + vec2(spread.x, -spread.y) * 0.7).r * 0.075;
    held += texture(Sampler2, source - vec2(spread.x, -spread.y) * 0.7).r * 0.075;

    held = max(0.0, held * decay - 0.006);

    float now = handMask(uv, texel);

    fragColor = vec4(max(now, held), now, 0.0, 1.0);
}
