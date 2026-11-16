#version 330

uniform sampler2D SceneSampler;

layout(std140) uniform FilterData {
    vec4 primary;    // x: width, y: height, z: brightness, w: contrast
    vec4 secondary;  // x: saturation, y: exposure, z: gamma, w: vignette
    vec4 tertiary;   // x: grain, y: tintIntensity, z: time, w: unused
    vec4 tintColor;  // rgb, a
};

in vec2 texCoord;
out vec4 fragColor;

vec3 applySaturation(vec3 color, float saturation) {
    float grey = dot(color, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(grey), color, saturation);
}

float noise(vec2 uv) {
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec4 scene = texture(SceneSampler, texCoord);
    vec3 color = scene.rgb;

    // 1. Brightness
    color += vec3(primary.z);

    // 2. Contrast
    color = (color - 0.5) * primary.w + 0.5;

    // 3. Saturation (default 1.2 for lush, rich visuals)
    color = applySaturation(color, secondary.x);

    // 4. Exposure & Gamma
    if (abs(secondary.y - 1.0) > 0.001) {
        color *= secondary.y;
    }
    if (abs(secondary.z - 1.0) > 0.001) {
        color = pow(max(color, vec3(0.0)), vec3(1.0 / max(secondary.z, 0.01)));
    }

    // 5. Custom Color Tint
    if (tertiary.y > 0.0) {
        color = mix(color, tintColor.rgb, tertiary.y * tintColor.a);
    }

    // 6. Vignette
    if (secondary.w > 0.0) {
        vec2 centered = texCoord * 2.0 - 1.0;
        float vignette = 1.0 - dot(centered, centered) * secondary.w * 0.55;
        color *= clamp(vignette, 0.0, 1.0);
    }

    // 7. Film grain
    if (tertiary.x > 0.0) {
        float grain = (noise(gl_FragCoord.xy + tertiary.z * 60.0) - 0.5) * tertiary.x * 0.08;
        color += vec3(grain);
    }

    fragColor = vec4(clamp(color, 0.0, 1.0), scene.a);
}
