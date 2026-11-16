#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float dist = clamp(length(texCoord0 - 0.5) * 2.0, 0.0, 1.0);
    float falloff = 1.0 - dist;
    float alpha = falloff * falloff * (0.6 + 0.4 * falloff);
    if (alpha <= 0.002) {
        discard;
    }
    vec4 base = texture(Sampler0, texCoord0) * vertexColor;
    fragColor = vec4(base.rgb, base.a * alpha) * ColorModulator;
}
