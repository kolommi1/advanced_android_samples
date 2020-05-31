precision mediump float;
varying vec3 vertColor; // vstup - barva z vertex shaderu
void main() {
	gl_FragColor = vec4(vertColor, 1.0); // výstup - barva fragmentu
} 
