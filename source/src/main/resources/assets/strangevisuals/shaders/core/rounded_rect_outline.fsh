#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 quadCoord;
in vec2 params;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 pixels = 1.0 / max(fwidth(quadCoord), vec2(0.0001));
    vec2 point = (quadCoord - 0.5) * pixels;
    vec2 half_size = pixels * 0.5;
    float radius = min(params.x, min(half_size.x, half_size.y));
    float thickness = max(params.y, 0.5);

    vec2 delta = abs(point) - half_size + radius;
    float dist = length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;

    float aa = 0.6;
    float outer = 1.0 - smoothstep(-aa, aa, dist);
    float inner = 1.0 - smoothstep(-aa, aa, dist + thickness);
    float ring = clamp(outer - inner, 0.0, 1.0);

    if (ring <= 0.001) {
        discard;
    }

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * ring);
}
