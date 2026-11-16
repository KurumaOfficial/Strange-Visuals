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

float median(vec3 value) {
    return max(min(value.r, value.g), min(max(value.r, value.g), value.b));
}

void main() {
    vec3 field = texture(Sampler0, texCoord0).rgb;
    float distance = median(field) - 0.5;
    vec2 unitRange = vec2(10.0) / vec2(textureSize(Sampler0, 0));
    vec2 texelsPerPixel = vec2(1.0) / fwidth(texCoord0);
    float pixelRange = max(0.5 * dot(unitRange, texelsPerPixel), 1.0);
    float alpha = clamp(distance * pixelRange + 0.5, 0.0, 1.0);
    if (alpha <= 0.001) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha) * ColorModulator;
}
