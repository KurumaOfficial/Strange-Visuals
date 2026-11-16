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

vec3 hueToRgb(float h) {
    vec3 k = fract(vec3(h) + vec3(0.0, 2.0 / 3.0, 1.0 / 3.0));
    return clamp(abs(k * 6.0 - 3.0) - 1.0, 0.0, 1.0);
}

void main() {
    vec2 pixels = 1.0 / max(fwidth(texCoord0), vec2(0.0001));
    vec2 point = (texCoord0 - 0.5) * pixels;
    vec2 half_size = pixels * 0.5;
    float radius = min(half_size.x, half_size.y);

    vec2 delta = abs(point) - half_size + radius;
    float dist = length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;
    float coverage = 1.0 - smoothstep(-0.6, 0.6, dist);
    if (coverage <= 0.001) {
        discard;
    }

    vec4 tex = texture(Sampler0, texCoord0);
    fragColor = vec4(hueToRgb(texCoord0.x) * tex.rgb, coverage * vertexColor.a * tex.a) * ColorModulator;
}
