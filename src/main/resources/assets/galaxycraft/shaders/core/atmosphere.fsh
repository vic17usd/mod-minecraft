#version 150
// Integración de dispersión simple con densidad exponencial y fase Henyey–Greenstein.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec3 SunDirection;
uniform vec3 BetaR;
uniform vec3 BetaM;
uniform vec3 Absorption;
uniform float PlanetRadius;
uniform float AtmosphereHeight;
uniform float ObserverHeight;
uniform float ScaleR;
uniform float ScaleM;
uniform float MieG;
uniform int Samples;
in vec2 texCoord;
in vec3 skyDirection;
out vec4 fragColor;
const float PI = 3.14159265;
float exitDistance(vec3 p, vec3 d, float radius) {
    float b = dot(p, d);
    return max(0.0, -b + sqrt(max(0.0, b*b - dot(p,p) + radius*radius)));
}
vec2 density(vec3 p) {
    float height = max(0.0, length(p) - PlanetRadius);
    return exp(-height / vec2(ScaleR, ScaleM));
}
void main() {
    vec3 background = texture(Sampler0, texCoord).rgb;
    vec3 d = normalize(skyDirection);
    if (AtmosphereHeight < 0.01) { fragColor = vec4(background, 1.0) * ColorModulator; return; }
    vec3 sun = normalize(SunDirection);
    vec3 origin = vec3(0.0, PlanetRadius + ObserverHeight, 0.0);
    float stepSize = min(1200.0, exitDistance(origin, d, PlanetRadius + AtmosphereHeight)) / float(Samples);
    // El hemisferio bajo el horizonte lo oculta el planeta sólido, no un segundo cielo invertido.
    if (d.y < -0.02) { fragColor = vec4(0.003,0.003,0.004,1.0); return; }
    vec2 viewDepth = vec2(0.0);
    vec3 scatterR = vec3(0.0), scatterM = vec3(0.0);
    for (int i=0; i<8; i++) {
        if (i>=Samples) break;
        vec3 p = origin + d * (float(i)+0.5) * stepSize;
        vec2 den = density(p);
        viewDepth += den * stepSize;
        float b = dot(p,sun);
        bool shadow = b<0.0 && b*b-dot(p,p)+PlanetRadius*PlanetRadius>0.0;
        if (!shadow) {
            float lightStep=exitDistance(p,sun,PlanetRadius+AtmosphereHeight)/4.0;
            vec2 lightDepth=vec2(0.0);
            for(int j=0;j<4;j++) lightDepth+=density(p+sun*(float(j)+0.5)*lightStep)*lightStep;
            vec3 attenuation=exp(-BetaR*(viewDepth.x+lightDepth.x)-(BetaM+Absorption)*(viewDepth.y+lightDepth.y));
            scatterR+=attenuation*den.x*stepSize;
            scatterM+=attenuation*den.y*stepSize;
        }
    }
    float mu=clamp(dot(d,sun),-1.0,1.0);
    float phaseR=3.0/(16.0*PI)*(1.0+mu*mu);
    float phaseM=(1.0-MieG*MieG)/(4.0*PI*pow(max(0.001,1.0+MieG*MieG-2.0*MieG*mu),1.5));
    vec3 transmission=exp(-BetaR*viewDepth.x-(BetaM+Absorption)*viewDepth.y);
    vec3 scatter=14.0*(scatterR*BetaR*phaseR+scatterM*BetaM*phaseM);
    fragColor=vec4(clamp(background*transmission+vec3(1.0)-exp(-scatter),0.0,1.0),1.0)*ColorModulator;
}
