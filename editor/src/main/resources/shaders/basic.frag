#version 330 core

in vec3 v_Color;
in vec2 v_TexCoord;

uniform sampler2D u_Albedo;
uniform vec4 u_Tint;

out vec4 o_Color;

void main() {
    vec4 textureColor = texture(u_Albedo, v_TexCoord);

    o_Color = textureColor * vec4(v_Color, 1.0) * u_Tint;
}