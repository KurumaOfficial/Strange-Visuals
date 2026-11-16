#version 330

layout(std140) uniform Uniforms {
    vec4 uScreen;
    vec4 uTint;
    vec4 uEvent[4];
    vec4 uPhase[4];
    mat4 uInverse;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 uv;
out vec4 fragColor;

const float PI = 3.14159265;

vec3 relativeAt(vec2 at, float depth) {
    vec4 clip = vec4(at * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 point = uInverse * clip;
    return point.xyz / point.w;
}

float hash21(vec2 p) {
    p = fract(p * vec2(443.8975, 397.2973));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float hash31(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash31(i);
    float n100 = hash31(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(i + vec3(1.0, 1.0, 1.0));
    return mix(mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
               mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y), f.z);
}

float fbm2(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int k = 0; k < 3; k++) {
        v += a * noise2(p);
        p *= 2.07;
        a *= 0.5;
    }
    return v;
}

float fbm3(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int k = 0; k < 3; k++) {
        v += a * noise3(p);
        p *= 2.11;
        a *= 0.5;
    }
    return v;
}

vec3 turn(vec3 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(p.x * c - p.z * s, p.y, p.x * s + p.z * c);
}

vec3 portal(vec3 ray, float sceneAway, vec3 centre, float size, float progress, float time,
            vec3 tint) {
    float appear = smoothstep(0.0, 0.10, progress);
    float leave = 1.0 - smoothstep(0.62, 1.0, progress);
    float alive = appear * leave;
    if (alive <= 0.002) {
        return vec3(0.0);
    }

    float radius = size * (0.35 + 0.65 * smoothstep(0.0, 0.32, progress))
        * (0.94 + 0.06 * sin(time * 3.1));
    float along = dot(ray, centre);
    if (along <= 0.0) {
        return vec3(0.0);
    }
    float perpSq = max(dot(centre, centre) - along * along, 0.0);
    float perp = sqrt(perpSq);

    float clear = step(along - radius, sceneAway);
    if (clear <= 0.0 && perp > radius * 3.0) {
        return vec3(0.0);
    }

    vec3 out0 = vec3(0.0);

    float inside = exp(-pow(perp / max(radius, 0.001), 2.0) * 2.6);
    out0 += tint * inside * 0.5 * alive * clear;
    out0 += vec3(1.0) * pow(inside, 6.0) * (1.2 - 0.7 * progress) * alive * clear;

    if (perp < radius && clear > 0.0) {
        float root = sqrt(max(radius * radius - perpSq, 0.0));
        float near = along - root;
        if (near > 0.0 && near < sceneAway) {
            vec3 normal = normalize(ray * near - centre);
            vec3 spun = turn(normal, time * 0.45);

            float lonf = fract(atan(spun.z, spun.x) * (14.0 / (2.0 * PI)) + time * 0.05);
            float latf = fract(asin(clamp(spun.y, -1.0, 1.0)) * (9.0 / PI));
            float grid = max(smoothstep(0.90, 1.0, max(lonf, 1.0 - lonf)),
                             smoothstep(0.93, 1.0, max(latf, 1.0 - latf)));

            float fil = smoothstep(0.52, 0.86, fbm3(spun * 3.4 + vec3(0.0, time * 0.7, 0.0)));
            float motes = smoothstep(0.88, 1.0, fbm3(spun * 11.0 - vec3(0.0, time * 1.6, 0.0)));

            float rim = pow(1.0 - abs(dot(normal, ray)), 3.2);

            vec3 shell = tint * (grid * 0.9 + fil * 0.55 + rim * 1.1)
                + vec3(1.0) * (motes * 0.5 + rim * rim * 0.35);
            out0 += shell * alive;
        }
    }

    float shockAt = radius * (1.0 + 2.6 * progress);
    float shock = exp(-pow((perp - shockAt) / (radius * 0.14), 2.0));
    out0 += tint * shock * (1.0 - progress) * 0.9 * alive * step(along, sceneAway);

    return out0;
}

vec3 tornado(vec3 ray, float sceneAway, vec3 centre, float size, float progress, float height,
             float time, vec3 tint) {
    float appear = smoothstep(0.0, 0.08, progress);
    float leave = 1.0 - smoothstep(0.62, 1.0, progress);
    float alive = appear * leave;
    if (alive <= 0.002) {
        return vec3(0.0);
    }

    float wide = size * 1.15;
    float tall = height;

    float bound = wide * 2.1;
    vec2 flat0 = ray.xz;
    float a = dot(flat0, flat0);
    if (a < 1.0e-8) {
        return vec3(0.0);
    }
    float b = -dot(flat0, centre.xz);
    float c = dot(centre.xz, centre.xz) - bound * bound;
    float under = b * b - a * c;
    if (under <= 0.0) {
        return vec3(0.0);
    }
    float root = sqrt(under);
    float t0 = (-b - root) / a;
    float t1 = (-b + root) / a;
    if (t1 <= 0.0) {
        return vec3(0.0);
    }
    t0 = max(t0, 0.05);

    if (abs(ray.y) > 1.0e-5) {
        float ta = (centre.y - 0.0) / ray.y;
        float tb = (centre.y + tall) / ray.y;
        t0 = max(t0, min(ta, tb));
        t1 = min(t1, max(ta, tb));
    } else if (centre.y > 0.0 || centre.y + tall < 0.0) {
        return vec3(0.0);
    }
    t1 = min(t1, sceneAway);
    if (t1 <= t0) {
        return vec3(0.0);
    }

    float grown = smoothstep(0.0, 0.30, progress);
    float top = tall * (0.15 + 0.85 * grown);

    const int STEPS = 26;
    float step0 = (t1 - t0) / float(STEPS);

    float jitter = hash21(gl_FragCoord.xy + vec2(time)) * step0;
    vec3 sum = vec3(0.0);
    float thick = 0.0;

    for (int i = 0; i < STEPS; i++) {
        float t = t0 + step0 * float(i) + jitter;
        if (t > t1 || thick > 0.98) {
            break;
        }
        vec3 at = ray * t;
        float up = at.y - centre.y;
        if (up < 0.0 || up > top) {
            continue;
        }
        float h = up / max(tall, 0.001);
        vec2 rel = at.xz - centre.xz;
        float span = wide * mix(0.10, 1.0, pow(h, 1.35));
        float r = length(rel) / max(span, 1.0e-4);

        float wall = exp(-pow((r - 0.78) / 0.30, 2.0)) * (1.0 - smoothstep(1.35, 1.95, r));
        float angle = atan(rel.y, rel.x);
        float swirl = fbm2(vec2(angle * 1.7 + h * 5.5 - time * 2.4, h * 4.2 - time * 1.1));
        float bands = smoothstep(0.30, 0.78, swirl);
        float lip = 1.0 - smoothstep(top - tall * 0.12, top, up);
        float foot = smoothstep(0.0, tall * 0.05, up);

        float dens = wall * bands * lip * foot;

        float throat = exp(-r * r * 5.5) * lip * foot * (0.5 + 0.5 * sin(time * 3.0 + h * 9.0));

        float take = (dens * 0.55 + throat * 0.5) * step0 * 2.4 * alive;
        vec3 shade = tint * (0.55 + 0.45 * bands) + vec3(1.0) * throat * 0.6;
        sum += shade * take * (1.0 - thick);
        thick += take;
    }

    float ringAt = wide * (0.6 + 2.4 * progress);
    float along = dot(ray, centre);
    float perp = sqrt(max(dot(centre, centre) - along * along, 0.0));
    float ring = exp(-pow((perp - ringAt) / (wide * 0.16), 2.0)) * (1.0 - progress);
    sum += tint * ring * 0.5 * alive * step(along, sceneAway) * step(0.0, along);

    return sum;
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
    int count = int(uScreen.w + 0.5);
    vec3 result = scene;

    for (int i = 0; i < 4; i++) {
        if (i >= count) {
            break;
        }
        vec3 centre = uEvent[i].xyz;
        float size = uEvent[i].w;
        float progress = uPhase[i].x;
        float style = uPhase[i].y;
        float height = uPhase[i].z;
        if (size <= 0.0 || progress >= 1.0) {
            continue;
        }
        if (style < 0.5) {
            result += portal(ray, sceneAway, centre, size, progress, time, uTint.rgb);
        } else {
            result += tornado(ray, sceneAway, centre, size, progress, height, time, uTint.rgb);
        }
    }

    fragColor = vec4(result, 1.0);
}
