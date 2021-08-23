#version 330 core
layout (location = 0) in vec3 aPos;

out vec3 TexCoords;

uniform mat4 projection;
uniform mat4 view;

uniform int chaos;
uniform int confuse;
uniform int shake;
uniform float time;

void main() {
    TexCoords = aPos;
    vec4 pos = projection * view * vec4(aPos, 1.0);
    gl_Position = pos.xyww;

    if (chaos == 1) {
        float strength = 0.3;
        vec3 pos = vec3(aPos.x + sin(time) * strength, aPos.y + cos(time) * strength, aPos.z + sin(time) * strength);
        TexCoords = pos;
    } else {
        TexCoords = aPos;
    }

    if (shake == 1) {
        float strength = 15;

        TexCoords.x += cos(time * 10) * strength;
        TexCoords.y += cos(time * 15) * strength;
        TexCoords.z += cos(time * 10) * strength;
    }
}
