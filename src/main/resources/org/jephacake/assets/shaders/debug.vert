#version 330 core
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUV;
layout(location = 3) in vec4 aLight; // per-vertex RGBA lighting (or color)

out vec2 vUV;
out vec3 vNormal;
out vec4 vLight;

uniform mat4 uProjection;
uniform mat4 uView;
uniform mat4 uModel;

void main() {
    gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
    vUV = aUV;
    vNormal = mat3(uModel) * aNormal;
    vLight = aLight;
}
