#version 330

#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D iChannel0;
uniform sampler2D iChannel1;
uniform vec2 iResolution;
uniform vec2 sz;
uniform float bfTiles;
uniform float rnd;

uniform float repeatLSize;
uniform float repeatL[40];

uniform float layer;
uniform float depthScale;
uniform float cameraTiltScale;
uniform float outsideScale;

uniform float backRotation;

uniform float effectColor;

uniform float iTime;
uniform vec2 iMouse;

in vec4 tint;
in vec2 uvIn;

out vec4 FragColor;

#define rlsz vec2(sz.x + 2.0 * bfTiles, sz.y + 2.0 * bfTiles)

#define sublayer ((layer - 1) * 10)

vec2 rotatePoint(vec2 point, vec2 origin, float angle, float aspectRatio) {
	float s = sin(angle);
	float c = cos(angle);

	point -= origin;
	point.x *= aspectRatio;
	
	point = vec2(point.x * c - point.y * s, point.x * s + point.y * c);
	
	point.x /= aspectRatio;
	point += origin;

	return point;
}

vec2 uvOf(vec2 uv, out vec2 uvScale, out int r, out float curRand) {
	ivec2 size = textureSize(iChannel0, 0);
	float layerHeight = (20.0 * rlsz.y) / float(size.y);
	float normalHeight = (rlsz.y * 20.0) / float(size.y);
	
	float ar = iResolution.x / iResolution.y;
	float pieceAr = float(sz.x) / float(sz.y);
	
	float idealWidthScale = float((int(floor(sz.x)) + (2 * int(floor(bfTiles)))) * 20) / float(size.x);
	idealWidthScale *= rnd * (effectColor > 1.0 ? 2.0 : 1.0);
	
	uvScale = vec2(ar / pieceAr / float(rnd), normalHeight);
	if (ar / pieceAr < 1.0) {
		uvScale = vec2(1.0 / float(rnd), normalHeight / (ar / pieceAr));
	}
	
	uvScale.x *= idealWidthScale;
	
	if (effectColor > 1.0) {
		uvScale.x /= 2.0;
	}
	
	vec2 uvOut = uv * uvScale;

	if (ar / pieceAr < 1.0) {
		uvOut.y -= (uvScale.y / 2.0) - (layerHeight / 2.0);
	} else {
		uvOut.x -= (uvScale.x / 2.0) - (((20.0 * rlsz.x) / float(size.x)) / 2.0);
	}

	r = int(mod(floor(iTime), float(rnd)));
	curRand = float(r) * ((20.0 * rlsz.x) / float(size.x));
	uvOut.x += curRand;
	
	return uvOut;
}

float calDepths() {
    float sum = 0.0;
    for (int i = 0; i < 40; ++i) {
        sum += repeatL[i];
    }
    return sum;
}

void main() {
	vec2 fragCoord = gl_FragCoord.xy;
	ivec2 size = textureSize(iChannel0, 0);
	float topOffset = 1.0 / float(size.y);
	int idealHeight = 1 + (int(floor(sz.y)) + (2 * int(floor(bfTiles)))) * int(floor(repeatLSize)) * 20;
	float idealWidthScale = float((int(floor(sz.x)) + (2 * int(floor(bfTiles)))) * 20) / float(size.x);
	float idealHeightScale = float(1 + (int(floor(sz.y)) + (2 * int(floor(bfTiles)))) * int(floor(repeatLSize)) * 20) / float(size.y);
	float previewOffset = float(size.y - idealHeight) / float(size.y);
	float layerHeight = (20.0 * rlsz.y) / float(size.y);
	float verticalOffset = (rlsz.y * topOffset) * 20.0;
	float normalHeight = (rlsz.y * 20.0) / float(size.y);
	
	float ar = iResolution.x / iResolution.y;
	float pieceAr = float(sz.x) / float(sz.y);
	
	vec2 uvScale;
	int r;
	float curRand;
	vec2 uv = uvOf(uvIn, uvScale, r, curRand);

	vec2 dummyA;
	int dummyB;
	float dummyC;
	vec2 uvOf1 = uvOf(vec2(1.0), dummyA, dummyB, dummyC);
	vec2 uvOf05 = uvOf(vec2(0.5), dummyA, dummyB, dummyC);
	
	float bottomBound = previewOffset;
	bool hit = false;
	int curDepth = 0;
	float totalDepth = calDepths();

	for (int i = 0; i < repeatLSize; i++) {
		for (int j = 0; j < int(floor(repeatL[int(floor(repeatLSize - i - 1))])); j++) {
			
			vec2 uvToUse = uv;
			uvToUse = rotatePoint(uvToUse, uvOf05, backRotation * ((totalDepth - curDepth - 5) / totalDepth), (uvScale.y / uvScale.x) * ar);
			
			vec2 tileSample = vec2(uvToUse.x, uvToUse.y + previewOffset + layerHeight * float(i));
			vec2 mouseOffset = vec2((iMouse.x - 0.5) / idealWidthScale, (iMouse.y - 0.5) / idealHeightScale) / vec2(cameraTiltScale, cameraTiltScale);
			vec2 depthOffset = vec2((float(repeatLSize - curDepth) / depthScale), (float(repeatLSize - curDepth) / depthScale)) / vec2(float(rnd), 1.0);
			vec2 fullOffset = mouseOffset * depthOffset;
			
			fullOffset *= uvScale;
			
			vec2 samplePos = tileSample + fullOffset;
			
			samplePos.y = 1.0 - samplePos.y;
			vec3 col = texture2D(iChannel0, samplePos).xyz;
			samplePos.y = 1.0 - samplePos.y;
			
			if (samplePos.y <= bottomBound || samplePos.y >= (bottomBound + layerHeight) || samplePos.y < 0.0 || samplePos.x < curRand || samplePos.x > ((effectColor > 1.0 ? 0.5 : 1.0) / float(rnd) + curRand) || samplePos.y > 1.0) {
				curDepth++;
				continue;
			}
			
			if (col != vec3(1.0)) {
				float palY = col.r * 3.0 + col.g * 2.0 + col.b + 1.5;
				palY /= 16.0;
				
				int trueDepth = -(curDepth + int(mod(-sublayer + 20, 30))) - 1;
				trueDepth = int(mod(trueDepth, 30));
				
				float palX = float(trueDepth);
				palX /= 32.0;

				vec3 paltex = texture2D(iChannel1, vec2(palX, palY)).xyz;
				
				vec3 fogCol = texture2D(iChannel1, vec2(1.5 / 32.0, 0.5 / 16.0)).xyz;
				float fogAmount = texture2D(iChannel1, vec2(9.5 / 32.0, 0.5 / 16.0)).x;
				
				paltex = mix(paltex, fogCol, (fogAmount * float(trueDepth)) / 30.0);
				
				FragColor = vec4(paltex, 1.0);
				hit = true;
			}
			curDepth++;
		}
		bottomBound += layerHeight;
	}
	
	if (hit) {
		return;
	}

	FragColor = texture2D(iChannel1, vec2(0.0, 0.0));
}
