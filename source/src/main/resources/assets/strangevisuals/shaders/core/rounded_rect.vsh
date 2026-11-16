#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 quadCoord;
out vec2 params;
out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    quadCoord = UV0;
    float raw = Position.z;
    float rad = mod(raw, 1000.0);
    float thick = floor(raw / 1000.0);
    params = vec2(rad, thick);
    vertexColor = Color;
}
