attribute vec4 inPosition;
attribute vec2 inTexCoord;
varying vec2 varTextCoord;
uniform mat4 matMVP;
void main() {
  varTextCoord = inTexCoord;
  gl_Position = matMVP * inPosition;
}
