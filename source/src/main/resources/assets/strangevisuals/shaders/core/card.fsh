#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 quadCoord;
in vec2 params;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 pixels = 1.0 / max(fwidth(quadCoord), vec2(0.0001));
    vec2 point = (quadCoord - 0.5) * pixels;
    vec2 half_size = pixels * 0.5;
    float radius = min(params.x, min(half_size.x, half_size.y));

    vec2 delta = abs(point) - half_size + radius;
    float dist = length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;
    float aa = 0.6;
    float coverage = 1.0 - smoothstep(-aa, aa, dist);
    if (coverage <= 0.001) {
        discard;
    }

    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec3 scene = texSize.x > 2.0 ? texture(Sampler0, gl_FragCoord.xy / (texSize * 4.0)).rgb : vec3(0.08, 0.09, 0.11);

    vec3 tint = vec3(0.06, 0.065, 0.075);
    vec3 color = mix(scene, tint, 0.88);

    float band = 1.0 - smoothstep(0.0, 1.4, abs(dist));
    color = mix(color, vec3(1.0), band * 0.25);

    fragColor = vec4(color * vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
}
