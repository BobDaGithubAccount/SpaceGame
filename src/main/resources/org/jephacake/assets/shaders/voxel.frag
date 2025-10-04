#version 330 core
in vec2 vUV;
in vec3 vNormal;
in vec4 vLight;

out vec4 fragColor;

uniform sampler2D uTexture;
uniform vec3 uDirectionalLightDir; // direction of light
uniform vec3 uDirectionalLightColor;
uniform float uUseDynamicLight; // 0 or 1

void main() {
    vec4 tex = texture(uTexture, vUV);

    // per-vertex precomputed light RGB + alpha as intensity multiplier
    vec3 prelight = vLight.rgb * vLight.a;

    // simple lambert directional light (optional)
    vec3 n = normalize(vNormal);
    float lambert = max(dot(n, normalize(-uDirectionalLightDir)), 0.0);
    vec3 dyn = uDirectionalLightColor * lambert * uUseDynamicLight;

    vec3 finalColor = tex.rgb * (prelight + dyn);
    float finalAlpha = tex.a;


    fragColor = vec4(finalColor, finalAlpha);
//    fragColor = tex;
//    fragColor = vec4(vUV, 0, 1);
}
