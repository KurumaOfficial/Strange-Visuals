#version 330

uniform sampler2D Sampler0;

in vec2 uv;

out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));

    vec3 color = vec3(0.0);
    color += texture(Sampler0, uv + vec2(-1.0, -1.0) * texel).rgb;
    color += texture(Sampler0, uv + vec2(1.0, -1.0) * texel).rgb;
    color += texture(Sampler0, uv + vec2(-1.0, 1.0) * texel).rgb;
    color += texture(Sampler0, uv + vec2(1.0, 1.0) * texel).rgb;

    fragColor = vec4(color * 0.25, 1.0);
}
