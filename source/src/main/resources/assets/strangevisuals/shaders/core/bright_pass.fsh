#version 330

layout(std140) uniform Uniforms {
    vec4 uGrade;
    vec4 uTint;
    vec4 uBloom;
    vec4 uScreen;
};

uniform sampler2D Sampler0;

in vec2 uv;
out vec4 fragColor;

void main() {
    vec3 scene = texture(Sampler0, uv).rgb;
    float luma = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    float excess = max(luma - uBloom.x, 0.0);

    float weight = excess / max(luma, 0.0001);
    weight *= smoothstep(0.0, 0.35, excess);
    fragColor = vec4(scene * weight, 1.0);
}
