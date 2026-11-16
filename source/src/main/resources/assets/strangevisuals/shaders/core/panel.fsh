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
    vec2 pixels = 1.0 / max(fwidth(texCoord0), vec2(0.0001));
    vec2 point = (texCoord0 - 0.5) * pixels;
    vec2 half_size = pixels * 0.5;
    float radius = min(half_size.x, half_size.y);

    vec2 delta = abs(point) - half_size + radius;
    float dist = length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;

    float aa = 0.55;
    float coverage = 1.0 - smoothstep(-aa, aa, dist);
    if (coverage <= 0.001) {
        discard;
    }

    vec4 base = texture(Sampler0, texCoord0) * vertexColor;
    float top = clamp(0.5 - texCoord0.y, 0.0, 1.0);
    float edge = 1.0 - smoothstep(0.4, 1.8, abs(dist));
    vec3 color = base.rgb + top * 0.04 + edge * 0.06;

    fragColor = vec4(color, base.a * coverage) * ColorModulator;
}
