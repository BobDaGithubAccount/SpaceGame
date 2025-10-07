#version 330 core

in vec2 vUV;
in vec3 vNormal;
in vec4 vLight;

out vec4 fragColor;

uniform sampler2D uTexture;
uniform vec3 uDirectionalLightDir;
uniform vec3 uDirectionalLightColor;
uniform float uUseDynamicLight;

// Computes edge thickness in screen space
float wireFactor() {
    vec2 d = fwidth(vUV);
    vec2 a = smoothstep(vec2(0.0), d * 1.5, vUV);
    vec2 b = smoothstep(vec2(1.0) - d * 1.5, vec2(1.0), vUV);
    float edge = min(min(a.x, b.x), min(a.y, b.y));
    return edge;
}

void main() {
    vec4 tex = texture(uTexture, vUV);

    // precomputed vertex lighting
    vec3 prelight = vLight.rgb * vLight.a;

    // dynamic light (optional)
    vec3 n = normalize(vNormal);
    float lambert = max(dot(n, normalize(-uDirectionalLightDir)), 0.0);
    vec3 dyn = uDirectionalLightColor * lambert * uUseDynamicLight;

    vec3 baseColor = tex.rgb * (prelight + dyn);
    float alpha = tex.a;

    // --- Wireframe overlay ---
    float edge = wireFactor();
    vec3 wireColor = vec3(0.0); // black lines
    float lineStrength = 1.0 - edge; // invert so lines are dark
    lineStrength = smoothstep(0.0, 0.1, lineStrength); // soften edges

    vec3 finalColor = mix(wireColor, baseColor, 1.0 - lineStrength);
    fragColor = vec4(finalColor, alpha);
}
