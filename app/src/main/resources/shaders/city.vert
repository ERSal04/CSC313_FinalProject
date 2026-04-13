#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;
// location 2 removed — CityMesh doesn't have UVs yet
// will be added back when ObjMesh replaces CityMesh

uniform mat4  model;
uniform mat4  view;
uniform mat4  projection;
uniform float waterLevel;

out vec3  fragNormal;
out vec3  fragPos;
out float floodHeight;

void main() {
    fragPos    = vec3(model * vec4(position, 1.0));
    fragNormal = normalize(mat3(transpose(inverse(model))) * normal);
    floodHeight = clamp((waterLevel - position.y) / 10.0, 0.0, 1.0);

    gl_Position = projection * view * model * vec4(position, 1.0);
}