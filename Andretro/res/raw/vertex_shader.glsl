attribute vec2 pos;
attribute vec2 tex;

varying vec2 texCoord;

uniform float screenWidth;
uniform float screenHeight;
uniform float imageWidth;
uniform float imageHeight;
uniform float imageAspect;
uniform float imageAspectInvert;

const float textureSize = 1024.0;

void main()
{
    float outputAspect = screenWidth / screenHeight;
    float inputAspect = ((imageAspect <= 0.0) ? imageWidth / imageHeight : imageAspect);
    
    if(imageAspectInvert > 0.0)
    {
        inputAspect = 1.0 / inputAspect;
    }
    
    float width = screenWidth;
    float height = screenHeight;
    
    if(outputAspect < inputAspect)
    {
        height = width / inputAspect;
    }
    else
    {
        width = height * inputAspect;
    }
    
    gl_Position.x = (pos.x * width) / screenWidth;
    gl_Position.y = (pos.y * height) / screenHeight;
    gl_Position.z = 0.0;
    gl_Position.w = 1.0;
    
    
    // Scale tex coords to the image size. Subtract .5 from the right and bottom
    // coords before scaling. Should add .5 for the left and top coords, but doesn't.
    texCoord.x = ((tex.x * imageWidth) - (.5 * tex.x)) / textureSize;
    texCoord.y = ((tex.y * imageHeight) - (.5 * tex.y)) / textureSize;
}
