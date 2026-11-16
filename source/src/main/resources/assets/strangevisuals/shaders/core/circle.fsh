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
    vec2 p = texCoord0 - 0.5;
    float dist = length(p) - 0.49;
    float aa = max(fwidth(dist), 0.0008) * 0.55;
    float coverage = 1.0 - smoothstep(-aa, aa, dist);
    if (coverage <= 0.001) {
        discard;
    }

    vec4 base = texture(Sampler0, texCoord0) * vertexColor;
    float top = clamp(0.5 - p.y, 0.0, 1.0);
    vec3 color = base.rgb + top * 0.04;

    fragColor = vec4(color, base.a * coverage) * ColorModulator;
}
