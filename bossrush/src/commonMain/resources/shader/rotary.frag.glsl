uniform sampler2D u_texture;
uniform float u_time;
uniform float u_alpha;
uniform float u_scale;
varying lowp vec4 v_color;
varying vec2 v_texCoords;

const vec2 virtualScreenSize = vec2(426.0, 240.0);
const vec2 skewAdjustment = vec2(virtualScreenSize.x / virtualScreenSize.y, virtualScreenSize.y / virtualScreenSize.x);
const float heightToWidthRatio = virtualScreenSize.y / virtualScreenSize.x;
const float skewdOffset = (1.0 - heightToWidthRatio)/2.0;
const vec2 halfScreenSize = virtualScreenSize / 2.0;
const vec2 halfTexture = vec2(0.5, 0.5);

void main() {
    vec2 coordInSquare = vec2(v_texCoords.x, skewdOffset + v_texCoords.y * heightToWidthRatio);
    coordInSquare -= halfTexture;
    coordInSquare /= u_scale;
    float length = length(coordInSquare);
    float lengthSqrt = sqrt(length);
    float s = sin(u_time * lengthSqrt);
    float c = cos(u_time * lengthSqrt);
    coordInSquare *= mat2(c, s, -s, c);
    coordInSquare += halfTexture;
    coordInSquare.y = (coordInSquare.y - skewdOffset) / heightToWidthRatio;
    vec4 color =  texture2D(u_texture, coordInSquare);
    color.a *= u_alpha;
    gl_FragColor = color;
}
