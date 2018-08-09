package shape3d;

import java.util.ArrayList;
import java.util.Iterator;

import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PGraphicsOpenGL;
import shapes3d.Shape3D;
import shapes3d.utils.MeshSection;
import shapes3d.utils.Textures;

public abstract class Mesh2DCore extends Shape3D {
    public PVector[][] coord;
    public PVector[][] norm;
    protected float[] u;
    protected float[] v;
    protected float ewRepeats = 1.0F;
    protected float nsRepeats = 1.0F;
    protected int ewPieces;
    protected int nsPieces;
    protected int ewSteps;
    protected int nsSteps;
    protected float nearlyOne;
    protected ArrayList<MeshSection> sections;
    public MeshSection fullShape;

    public Mesh2DCore() {
    }

    protected void calcNormals() {
        for(int var4 = 0; var4 < this.nsSteps; ++var4) {
            for(int var5 = 0; var5 < this.ewSteps; ++var5) {
                PVector var1 = this.coord[var5][var4];
                PVector var2;
                if(var4 == this.nsSteps - 1) {
                    var2 = PVector.sub(var1, this.coord[var5][var4 - 1]);
                    var2.add(var1);
                } else {
                    var2 = this.coord[var5][var4 + 1];
                }

                PVector var3;
                if(var5 == this.ewSteps - 1) {
                    var3 = this.coord[1][var4];
                } else {
                    var3 = this.coord[var5 + 1][var4];
                }

                this.norm[var5][var4] = PVector.cross(PVector.sub(var3, var1, (PVector)null), PVector.sub(var2, var1, (PVector)null), (PVector)null);
                this.norm[var5][var4].normalize();
            }
        }

    }

    protected void calcUV(float var1, float var2) {
        this.u = new float[this.ewSteps];
        float var3 = var1 / (float)this.ewPieces;

        for(int var4 = 0; var4 < this.u.length; ++var4) {
            this.u[var4] = (float)var4 * var3;
        }

        this.v = new float[this.nsSteps];
        float var6 = var2 / (float)this.nsPieces;

        for(int var5 = 0; var5 < this.nsSteps; ++var5) {
            this.v[var5] = (float)var5 * var6;
        }

    }

    public MeshSection getDrawSection() {
        return new MeshSection(this.ewSteps, this.nsSteps);
    }

    public void addDrawSection(MeshSection var1) {
        if(this.sections == null) {
            this.sections = new ArrayList();
        }

        this.sections.add(var1);
    }

    public void removeDrawSection(MeshSection var1) {
        if(this.sections != null) {
            this.sections.remove(var1);
            if(this.sections.size() == 0) {
                this.sections = null;
            }
        }

    }

    public void setDrawFullShape() {
        if(this.sections != null) {
            this.sections.clear();
        }

        this.sections = null;
    }

    public void setTexture(String var1) {
        this.setTexture(var1, 1.0F, 1.0F);
    }

    public void setTexture(PImage var1) {
        this.setTexture(var1, 1.0F, 1.0F);
    }

    public void setTexture(String var1, float var2, float var3) {
        PImage var4 = Textures.loadImage(this.app, var1);
        this.setTexture(var4, var2, var3);
    }

    public void setTexture(PImage var1, float var2, float var3) {
        this.ewRepeats = var2;
        this.nsRepeats = var3;
        this.calcUV(this.ewRepeats, this.nsRepeats);
        this.skin = var1;
    }

    public void draw() {
        if(this.visible) {
            this.drawWhat();
            Iterator var1;
//            if(pickModeOn) {
//                pickBuffer.pushMatrix();
//                if(this.pickable && this.drawMode != 17) {
//                    this.drawForPicker(pickBuffer);
//                }
//
//                if(this.children != null) {
//                    var1 = this.children.iterator();
//
//                    while(var1.hasNext()) {
//                        ((Shape3D)var1.next()).drawForPicker(pickBuffer);
//                    }
//                }
//
//                pickBuffer.popMatrix();
//            }
//            else
                {
                this.app.pushStyle();
                this.app.pushMatrix();
                this.app.translate(this.pos.x, this.pos.y, this.pos.z);
                this.app.rotateX(this.rot.x);
                this.app.rotateY(this.rot.y);
                this.app.rotateZ(this.rot.z);
                this.app.scale(this.shapeScale);
                if(this.useTexture) {
                    this.drawWithTexture();
                } else {
                    this.drawWithoutTexture();
                }

                if(this.children != null) {
                    var1 = this.children.iterator();

                    while(var1.hasNext()) {
                        ((Shape3D)var1.next()).draw();
                    }
                }

                this.app.popMatrix();
                this.app.popStyle();
            }
        }

    }

//    protected void drawForPicker(PGraphicsOpenGL var1) {
//        if(this.drawMode != 17) {
//            if(this.sections != null) {
//                for(int var2 = 0; var2 < this.sections.size(); ++var2) {
//                    this.drawForPicker(pickBuffer, (MeshSection)this.sections.get(var2));
//                }
//            } else {
//                this.drawForPicker(pickBuffer, this.fullShape);
//            }
//        }
//
//    }

    protected void drawForPicker(PGraphicsOpenGL var1, MeshSection var2) {
        var1.pushMatrix();
        var1.translate(this.pos.x, this.pos.y, this.pos.z);
        var1.rotateX(this.rot.x);
        var1.rotateY(this.rot.y);
        var1.rotateZ(this.rot.z);
        var1.scale(this.shapeScale);
        var1.noStroke();

        for(int var4 = var2.sNS; var4 < var2.eNS - 1; ++var4) {
            var1.beginShape(10);
            var1.fill(this.pickColor);

            for(int var5 = var2.sEW; var5 < var2.eEW; ++var5) {
                PVector var3 = this.coord[var5][var4];
                var1.vertex(var3.x, var3.y, var3.z);
                var3 = this.coord[var5][var4 + 1];
                var1.vertex(var3.x, var3.y, var3.z);
            }

            var1.endShape(2);
        }

        var1.popMatrix();
    }

    protected void drawWithTexture() {
        this.app.textureMode(1);
        if(this.useSolid) {
            this.app.fill(this.fillColor);
        } else {
            this.app.noFill();
        }

        if(this.useWire) {
            this.app.stroke(this.strokeColor);
            this.app.strokeWeight(this.strokeWeight);
            this.app.hint(OPTIMIZED_STROKE);
        } else {
            this.app.noStroke();
        }

        this.app.textureMode(1);
        if(this.sections != null) {
            for(int var1 = 0; var1 < this.sections.size(); ++var1) {
                this.drawWithTexture((MeshSection)this.sections.get(var1));
            }
        } else {
            this.drawWithTexture(this.fullShape);
        }

        this.app.hint(-6);
    }

    protected void drawWithTexture(MeshSection var1) {
        for(int var4 = var1.sNS; var4 < var1.eNS - 1; ++var4) {
            this.app.beginShape(18);
            this.app.texture(this.skin);
            this.app.textureWrap(1);

            for(int var5 = var1.sEW; var5 < var1.eEW; ++var5) {
                PVector var2 = this.coord[var5][var4];
                PVector var3 = this.norm[var5][var4];
                this.app.normal(var3.x, var3.y, var3.z);
                this.app.vertex(var2.x, var2.y, var2.z, this.u[var5], this.v[var4]);
                var2 = this.coord[var5][var4 + 1];
                this.app.normal(var3.x, var3.y, var3.z);
                this.app.vertex(var2.x, var2.y, var2.z, this.u[var5], this.v[var4 + 1]);
            }

            this.app.endShape(2);
        }

    }

    protected void drawWithoutTexture() {
        if(this.useWire) {
            this.app.stroke(this.strokeColor);
            this.app.strokeWeight(this.strokeWeight);
            this.app.hint(OPTIMIZED_STROKE);
        } else {
            this.app.noStroke();
        }

        if(this.useSolid) {
            this.app.fill(this.fillColor);
        } else {
            this.app.noFill();
        }

        if(this.sections != null) {
            for(int var1 = 0; var1 < this.sections.size(); ++var1) {
                this.drawWithoutTexture((MeshSection)this.sections.get(var1));
            }
        } else {
            this.drawWithoutTexture(this.fullShape);
        }

        this.app.hint(-6);
    }

    protected void drawWithoutTexture(MeshSection var1) {
        for(int var4 = var1.sNS; var4 < var1.eNS - 1; ++var4) {
            this.app.beginShape(18);

            for(int var5 = var1.sEW; var5 < var1.eEW; ++var5) {
                PVector var2 = this.coord[var5][var4];
                PVector var3 = this.norm[var5][var4];
                this.app.normal(var3.x, var3.y, var3.z);
                this.app.vertex(var2.x, var2.y, var2.z);
                var2 = this.coord[var5][var4 + 1];
                var3 = this.norm[var5][var4 + 1];
                this.app.normal(var3.x, var3.y, var3.z);
                this.app.vertex(var2.x, var2.y, var2.z);
            }

            this.app.endShape(2);
        }

    }

    protected void drawNormals() {
        this.app.stroke(255.0F, 0.0F, 0.0F);

        for(int var3 = 0; var3 < this.nsSteps; ++var3) {
            for(int var4 = 0; var4 < this.ewSteps; ++var4) {
                PVector var2 = this.coord[var4][var3];
                PVector var1 = PVector.mult(this.norm[var4][var3], 20.0F);
                this.app.line(var2.x, var2.y, var2.z, var2.x + var1.x, var2.y + var1.y, var2.z + var1.z);
            }
        }

        this.app.noStroke();
    }
}
