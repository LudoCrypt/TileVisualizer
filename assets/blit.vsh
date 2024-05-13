#version 330

attribute vec4 Color;
attribute vec3 Pos;
attribute vec2 texCoord;

uniform mat4 projMat;

uniform float outsideScale;

out vec4 tint;
out vec2 uvIn;

void main() {
	tint = Color;
	uvIn = (texCoord * 2.0 - 1.0);
	uvIn += outsideScale * uvIn;
	uvIn = uvIn / 2.0 + 0.5;
	uvIn.y = 1.0 - uvIn.y;
	gl_Position = projMat * vec4(Pos, 1.0);
}