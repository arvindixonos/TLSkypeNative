package shape3d;

/**
 * Created by TakeLeap05 on 09-08-2018.
 */
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import processing.core.PApplet;
import processing.core.PVector;
import shapes3d.EndCapDisc;
import shapes3d.utils.I_RadiusGen;
import shapes3d.utils.MeshSection;
import shapes3d.utils.P_Bezier3D;
import shapes3d.utils.Rot;
import shapes3d.utils.TubeRadius;
import shapes3d.utils.VectorUtil;

public class BezTube extends shapes3d.Mesh2DCoreWithCaps {
    protected P_Bezier3D bz;
    protected I_RadiusGen tRad;

    public BezTube(PApplet var1, P_Bezier3D var2, float var3, int var4, int var5) {
        this.ctorCore(var1, var2, new TubeRadius(var3), var4, var5);
    }

    public BezTube(PApplet var1, P_Bezier3D var2, I_RadiusGen var3, int var4, int var5) {
        this.ctorCore(var1, var2, var3, var4, var5);
    }

    public BezTube(PApplet var1, P_Bezier3D var2, float var3, int var4, int var5, PVector var6, PVector var7) {
        this.ctorCore(var1, var2, new TubeRadius(var3), var4, var5, var6, var7);
    }

    public BezTube(PApplet var1, P_Bezier3D var2, I_RadiusGen var3, int var4, int var5, PVector var6, PVector var7) {
        this.ctorCore(var1, var2, var3, var4, var5, var6, var7);
    }

    private void ctorCore(PApplet var1, P_Bezier3D var2, I_RadiusGen var3, int var4, int var5) {
        this.app = var1;
        this.nsPieces = var4;
        this.ewPieces = var5;
        this.bz = var2;
        this.tRad = var3;
        this.calcShape();
        this.calcXYZ();
    }

    private void ctorCore(PApplet var1, P_Bezier3D var2, I_RadiusGen var3, int var4, int var5, PVector var6, PVector var7) {
        this.app = var1;
        this.nsPieces = var4;
        this.ewPieces = var5;
        this.bz = var2;
        this.tRad = var3;
        this.calcShape();
        this.shapeOrientation(var6, var7);
    }

    public void shapeOrientation(PVector var1, PVector var2) {
        boolean var3 = false;
        if(var1 != null && var1.mag() > 0.0F && !VectorUtil.same(this.up, var1)) {
            this.up.set(var1);
            var3 = true;
        }

        if(var2 != null && !VectorUtil.same(this.centreRot, var2)) {
            this.centreRot.set(var2);
            var3 = true;
        }

        if(var3) {
            Rot var4 = new Rot(new PVector(0.0F, 1.0F, 0.0F), this.up);
            PVector[] var5 = this.bz.getCtrlPointArray();

            for(int var6 = 0; var6 < var5.length; ++var6) {
                var4.applyTo(var5[var6]);
                var5[var6].add(this.centreRot);
            }

            this.setBez(new P_Bezier3D(var5, var5.length));
        }

        this.calcXYZ();
    }

    protected void calcShape() {
        this.nsSteps = this.nsPieces + 1;
        this.ewSteps = this.ewPieces + 1;
        this.coord = new PVector[this.ewSteps][this.nsSteps];
        this.norm = new PVector[this.ewSteps][this.nsSteps];
        this.fullShape = new MeshSection(this.ewSteps, this.nsSteps);
        this.startEC = new EndCapDisc(this);
        this.endEC = new EndCapDisc(this);
    }

    protected void calcXYZ() {
        PVector[] var1 = new PVector[this.ewSteps];
        PVector var2 = new PVector(0.0F, 1.0F, 0.0F);
        var1 = this.createCircleAboutAxis(var2);
        PVector[] var5 = this.getPoints(this.nsSteps);
        PVector[] var6 = this.getTangents(this.nsSteps);
        float var7 = 0.0F;
        float var8 = 1.0F / (float)(this.nsSteps - 1);
        Rot var3 = new Rot(var2, var6[0]);
        float var9 = this.tRad.radius(var7);

        PVector var4;
        int var10;
        for(var10 = 0; var10 < this.ewSteps; ++var10) {
            var4 = new PVector(var1[var10].x, var1[var10].y, var1[var10].z);
            var3.applyTo(var4);
            var4.normalize();
            var4.mult(var9);
            var4.add(var5[0]);
            this.coord[var10][0] = var4;
        }

        var7 += var8;

        for(var10 = 1; var10 < this.nsSteps; ++var10) {
            var9 = this.tRad.radius(var7);
            var3 = new Rot(var6[var10 - 1], var6[var10]);

            for(int var11 = 0; var11 < this.ewSteps; ++var11) {
                var4 = PVector.sub(this.coord[var11][var10 - 1], var5[var10 - 1]);
                var3.applyTo(var4);
                var4.normalize();
                var4.mult(var9);
                var4.add(var5[var10]);
                this.coord[var11][var10] = var4;
            }

            var7 += var8;
        }

        if(this.tRad.hasConstantRadius()) {
            this.calcNormals(var5);
        } else {
            this.calcNormals();
        }

        PVector[] var14 = new PVector[this.ewSteps];
        PVector[] var15 = new PVector[this.ewSteps];

        for(int var13 = 0; var13 < this.ewSteps; ++var13) {
            PVector var12 = this.coord[var13][0];
            var14[var13] = new PVector(var12.x, var12.y, var12.z);
            var12 = this.coord[var13][this.nsPieces];
            var15[var13] = new PVector(var12.x, var12.y, var12.z);
        }

//        this.startEC.calcShape(var15, this.ewSteps, 1);
//        this.endEC.calcShape(var14, this.ewSteps, -1);
    }

    protected PVector[] createCircleAboutAxis(PVector var1) {
        PVector[] var2 = new PVector[this.ewSteps];
        float var3 = 0.0F;
        float var4 = 6.2831855F / ((float)this.ewSteps - 1.0F);

        for(int var5 = 0; var5 < this.ewSteps - 1; ++var5) {
            var2[var5] = new PVector((float) Math.cos((double)var3), 0.0F, (float) Math.sin((double)var3));
            var3 -= var4;
        }

        var2[this.ewSteps - 1] = new PVector();
        var2[this.ewSteps - 1].set(var2[0]);
        PVector var8 = new PVector(0.0F, 1.0F, 0.0F);
        if(!VectorUtil.same(var8, var1)) {
            Rot var6 = new Rot(new PVector(0.0F, 1.0F, 0.0F), var1);

            for(int var7 = 0; var7 < this.ewSteps; ++var7) {
                var6.applyTo(var2[var7]);
            }
        }

        return var2;
    }

    public void setDetails(P_Bezier3D var1, float var2) {
        TubeRadius var3 = new TubeRadius(var2);
        this.setDetails(var1, var3);
    }

    public void setDetails(P_Bezier3D var1, I_RadiusGen var2) {
        while(this.drawLock) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException var4) {
                ;
            }
        }

        this.drawLock = true;
        this.bz = var1;
        this.tRad = var2;
        this.shapeOrientation(this.up, this.centreRot);
        this.drawLock = false;
    }

    public void setRadius(float var1) {
        TubeRadius var2 = new TubeRadius(var1);
        this.setRadius(var2);
    }

    public void setRadius(I_RadiusGen var1) {
        while(this.drawLock) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException var3) {
                ;
            }
        }

        this.drawLock = true;
        this.tRad = var1;
        this.calcXYZ();
        this.drawLock = false;
    }

    public void setBez(P_Bezier3D var1) {
        while(this.drawLock) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException var3) {
                ;
            }
        }

        this.drawLock = true;
        this.bz = var1;
        this.shapeOrientation(this.up, this.centreRot);
        this.drawLock = false;
    }

    public P_Bezier3D getBez() {
        return this.bz;
    }

    public float getRadius(float var1) {
        return this.tRad.radius(var1);
    }

    public I_RadiusGen getRadiusProfiler() {
        return this.tRad;
    }

    public PVector getTangent(float var1) {
        return this.bz.tangent(var1);
    }

    public PVector[] getTangents(int var1) {
        return this.bz.tangents(var1);
    }

    public PVector getPoint(float var1) {
        return this.bz.point(var1);
    }

    public PVector[] getPoints(int var1) {
        return this.bz.points(var1);
    }

    public PVector[] getNormals(int var1) {
        PVector[] var2 = new PVector[var1];
        float var3 = 1.0F / (float)(var1 - 1);
        float var4 = 0.0F;

        for(int var5 = 0; var5 < var1; ++var5) {
            var2[var5] = this.getNormal(var4);
            var4 += var3;
        }

        return var2;
    }

    public PVector getNormal(float var1) {
        var1 = PApplet.constrain(var1, 0.0F, 0.999999F);
        float var2 = var1 * (float)this.nsPieces;
        int var3 = (int)var2;
        float var4 = var2 - (float)var3;
        PVector var5 = PVector.sub(this.norm[0][var3 + 1], this.norm[0][var3]);
        var5.mult(var4);
        var5.add(this.norm[0][var3]);
        return var5;
    }

    protected void calcNormals(PVector[] var1) {
        for(int var2 = 0; var2 < this.nsSteps; ++var2) {
            for(int var3 = 0; var3 < this.ewSteps; ++var3) {
                this.norm[var3][var2] = PVector.sub(this.coord[var3][var2], var1[var2]);
                this.norm[var3][var2].normalize();
            }
        }

    }

    public void restoreShape() {
        this.calcXYZ();
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

    public void draw() {
        if(this.visible && !this.drawLock) {
            this.drawLock = true;
            super.draw();

//            MeshSection meshSection = this.fullShape;
//            drawWithoutTexture(meshSection);

//            this.app.pushStyle();
//            this.app.pushMatrix();
//            if(pickModeOn)
//            {
//                if(this.pickable && this.startEC.visible && this.startEC.capDrawMode != 17) {
//                    this.startEC.drawForPicker(pickBuffer);
//                }
//
//                if(this.pickable && this.endEC.visible && this.endEC.capDrawMode != 17) {
//                    this.endEC.drawForPicker(pickBuffer);
//                }
//            }
//            else
                {
//                this.app.translate(this.pos.x, this.pos.y, this.pos.z);
//                this.app.rotateX(this.rot.x);
//                this.app.rotateY(this.rot.y);
//                this.app.rotateZ(this.rot.z);
//                this.app.scale(this.shapeScale);
//                this.app.fill(this.fillColor);
//                if(this.startEC.visible) {
//                    this.startEC.draw();
//                }
//
//                if(this.endEC.visible) {
//                    this.endEC.draw();
//                }

//                if(this.children != null) {
//                    Iterator var1 = this.children.iterator();
//
//                    while(var1.hasNext()) {
//                        ((Shape3D)var1.next()).draw();
//                    }
//                }
            }

//            this.app.popMatrix();
//            this.app.popStyle();
            this.drawLock = false;
        }

    }
}
