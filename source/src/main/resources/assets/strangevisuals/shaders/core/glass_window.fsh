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
    float uBase = floor(texCoord0.x);
    vec2 normUV = vec2(clamp(texCoord0.x - uBase, 0.0, 1.0), clamp(texCoord0.y, 0.0, 1.0));
    vec2 pixels = 1.0 / max(fwidth(normUV), vec2(0.0001));
    vec2 point = (normUV - 0.5) * pixels;
    vec2 half_size = pixels * 0.5;
    float radiusScale = clamp(uBase, 0.0, 10.0) * 0.2;
    float radius = min(6.0 * radiusScale, min(half_size.x, half_size.y));

    vec2 delta = abs(point) - half_size + radius;
    float dist = length(max(delta, 0.0)) + min(max(delta.x, delta.y), 0.0) - radius;
    float aa = 0.6;
    float coverage = 1.0 - smoothstep(-aa, aa, dist);
    if (coverage <= 0.001) {
        discard;
    }

    vec2 backdropSize = vec2(textureSize(Sampler0, 0)) * 8.0;
    vec2 uv = gl_FragCoord.xy / backdropSize;
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));

    vec3 scene = vec3(0.0);
    scene += texture(Sampler0, uv + vec2(-texel.x, -texel.y)).rgb * 0.06;
    scene += texture(Sampler0, uv + vec2(0.0, -texel.y)).rgb * 0.12;
    scene += texture(Sampler0, uv + vec2(texel.x, -texel.y)).rgb * 0.06;
    scene += texture(Sampler0, uv + vec2(-texel.x, 0.0)).rgb * 0.12;
    scene += texture(Sampler0, uv).rgb * 0.28;
    scene += texture(Sampler0, uv + vec2(texel.x, 0.0)).rgb * 0.12;
    scene += texture(Sampler0, uv + vec2(-texel.x, texel.y)).rgb * 0.06;
    scene += texture(Sampler0, uv + vec2(0.0, texel.y)).rgb * 0.12;
    scene += texture(Sampler0, uv + vec2(texel.x, texel.y)).rgb * 0.06;

    vec3 tint = vec3(0.05, 0.055, 0.06);
    vec3 color = mix(scene, tint + scene * 0.3, 0.85);

    float top = pow(clamp(1.0 - normUV.y, 0.0, 1.0), 4.0);
    color += top * 0.06;

    float band = 1.0 - smoothstep(0.0, 1.8, abs(dist));
    color = mix(color, vec3(1.0), band * 0.34);

    fragColor = vec4(color * vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
}
