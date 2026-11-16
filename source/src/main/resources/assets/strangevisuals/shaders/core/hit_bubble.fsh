#version 330

layout(std140) uniform Uniforms {
    vec4 uScreen;
    vec4 uTint;
    vec4 uBubble[8];
    vec4 uFade[8];
    mat4 uInverse;
    mat4 uForward;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 uv;
out vec4 fragColor;

vec3 relativeAt(vec2 at, float depth) {
    vec4 clip = vec4(at * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 point = uInverse * clip;
    return point.xyz / point.w;
}

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

float noise(vec3 p) {
    vec3 cell = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash(cell);
    float n100 = hash(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash(cell + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float film(float thickness) {
    return 0.55 + 0.45 * cos(6.2831853 * thickness);
}

void main() {
    float depth = texture(Sampler1, uv).r;
    vec3 scene = texture(Sampler0, uv).rgb;
    vec3 at = relativeAt(uv, min(depth, 0.99999));
    float sceneAway = length(at);
    vec3 ray = at / max(sceneAway, 0.0001);
    if (depth > 0.99999) {
        sceneAway = 4096.0;
    }
    float time = uScreen.z;

    vec3 result = scene;
    int count = int(uScreen.w + 0.5);

    for (int i = 0; i < 8; i++) {
        if (i >= count) {
            break;
        }
        vec3 centre = uBubble[i].xyz;
        float radius = uBubble[i].w;
        float strength = uFade[i].x;
        float bend = uFade[i].y;
        float age = uFade[i].z;
        if (radius <= 0.0 || strength <= 0.003) {
            continue;
        }

        float b = dot(ray, centre);
        float c = dot(centre, centre) - radius * radius;
        float under = b * b - c;
        if (under <= 0.0) {
            continue;
        }
        float root = sqrt(under);
        float near = b - root;
        float far = b + root;
        if (far <= 0.0 || near >= sceneAway) {
            continue;
        }
        near = max(near, 0.0);

        vec3 surface = ray * near;
        vec3 normal = normalize(surface - centre);

        vec3 wobble = vec3(
            noise(normal * 3.1 + vec3(0.0, time * 0.55, 0.0)),
            noise(normal * 3.4 + vec3(time * 0.45, 0.0, 2.7)),
            noise(normal * 2.9 + vec3(5.1, 0.0, time * 0.5))) - 0.5;
        normal = normalize(normal + wobble * 0.22);

        float facing = clamp(abs(dot(normal, ray)), 0.0, 1.0);
        float grazing = 1.0 - facing;

        float drain = 0.55 + normal.y * 0.28 + age * 0.5;
        float crawl = noise(normal * 2.2 + vec3(time * 0.25, time * 0.18, 0.0)) * 0.5;
        float thickness = (drain + crawl) / max(0.22, facing);

        vec3 sheen = uTint.rgb * (0.7 + 0.6 * film(thickness));

        vec3 into = refract(ray, normal, 1.0 / 1.34);
        float back = -2.0 * dot(into, surface - centre);
        vec3 exitAt = surface + into * max(back, 0.001);
        vec3 exitNormal = normalize(exitAt - centre);
        vec3 outward = refract(into, -exitNormal, 1.34);
        if (dot(outward, outward) < 0.0001) {
            outward = reflect(into, -exitNormal);
        }
        outward = normalize(mix(ray, outward, clamp(bend * 26.0, 0.0, 1.0)));

        float carry = max(1.0, min(sceneAway, 48.0) - near) * (1.0 + near * 0.45);
        vec4 landed = uForward * vec4(exitAt + outward * carry, 1.0);
        vec2 through = landed.w > 0.0001 ? landed.xy / landed.w * 0.5 + 0.5 : uv;
        vec3 behind = texture(Sampler0, clamp(through, vec2(0.002), vec2(0.998))).rgb;

        float edge = pow(grazing, 2.4);

        float presence = strength * (0.62 + edge * 0.38);

        vec3 wall = mix(behind, behind * 0.92 + sheen * 0.4, clamp(0.1 + edge, 0.0, 1.0));

        vec3 keyLight = normalize(vec3(-0.4, 0.75, -0.5));
        vec3 fillLight = normalize(vec3(0.6, 0.3, 0.6));
        float key = pow(max(dot(reflect(ray, normal), keyLight), 0.0), 90.0);
        float fill = pow(max(dot(reflect(ray, normal), fillLight), 0.0), 40.0) * 0.35;

        result = mix(result, wall, presence);
        result += sheen * edge * strength * 0.35;
        result += uTint.rgb * edge * strength * 0.25;
        result += vec3(1.0) * (key + fill) * strength;
    }

    fragColor = vec4(result, 1.0);
}
