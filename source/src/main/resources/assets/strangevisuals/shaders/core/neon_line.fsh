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
    float across = clamp(abs(p.y) * 2.0, 0.0, 1.0);
    float along = clamp(abs(p.x) * 2.0, 0.0, 1.0);

    float core = 1.0 - smoothstep(0.0, 0.22, across);
    float bloom = pow(1.0 - across, 3.0);
    float taper = pow(1.0 - along, 0.7);

    float alpha = (core + bloom * 0.55) * taper;
    if (alpha <= 0.002) {
        discard;
    }

    vec4 base = texture(Sampler0, texCoord0) * vertexColor;
    vec3 color = base.rgb + core * 0.35;
    fragColor = vec4(color, clamp(base.a * alpha, 0.0, 1.0)) * ColorModulator;
}
