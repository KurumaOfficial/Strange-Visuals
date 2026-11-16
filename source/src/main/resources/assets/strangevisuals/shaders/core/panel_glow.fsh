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
    float spread = max(pixels.y * 0.25, 0.5);

    vec2 point = (texCoord0 - 0.5) * pixels;
    vec2 half_size = max(pixels * 0.5 - spread, vec2(0.0));
    float radius = min(half_size.x, half_size.y);

    vec2 delta = abs(point) - half_size + radius;
    float dist = length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;

    float falloff = 1.0 - clamp(dist / spread, 0.0, 1.0);
    float alpha = falloff * falloff * (0.5 + 0.5 * falloff);
    if (alpha <= 0.002) {
        discard;
    }

    vec4 base = texture(Sampler0, texCoord0) * vertexColor;
    fragColor = vec4(base.rgb, base.a * alpha) * ColorModulator;
}
