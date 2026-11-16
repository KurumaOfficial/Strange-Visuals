#version 330

layout(std140) uniform Uniforms {
    vec4 uGrade;
    vec4 uTint;
    vec4 uBloom;
    vec4 uScreen;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 uv;
out vec4 fragColor;

void main() {
    vec3 colour = texture(Sampler0, uv).rgb;

    vec3 glow = texture(Sampler1, uv).rgb;
    colour += glow * uBloom.y;

    colour *= uGrade.x;

    colour = (colour - 0.5) * uGrade.y + 0.5;

    float luma = dot(colour, vec3(0.2126, 0.7152, 0.0722));
    colour = mix(vec3(luma), colour, uGrade.z);

    float shadow = 1.0 - smoothstep(0.0, 0.75, luma);
    colour = mix(colour, colour * uTint.rgb + uTint.rgb * 0.06, shadow * uTint.w);

    vec2 centred = uv - 0.5;
    centred.x *= uScreen.x / max(uScreen.y, 1.0);
    float vignette = 1.0 - smoothstep(0.35, 1.15, length(centred) * 1.35) * uGrade.w;
    colour *= vignette;

    fragColor = vec4(max(colour, vec3(0.0)), 1.0);
}
