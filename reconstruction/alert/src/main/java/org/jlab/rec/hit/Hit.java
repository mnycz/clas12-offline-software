package org.jlab.rec.hit;

import org.jlab.detector.geant4.v2.ALERTGeant4Factory;
import org.jlab.geom.prim.Point3D;

public abstract class Hit implements Comparable<Hit> {
    //Comparable is used for sorting
    private int _Id;
    private int _Sector;
    private int _Superlayer;
    private int _Layer;
    private int _Paddle;
    private int _ADC1; // ADC Front
    private int _ADC2; // ADC Back
    private int _ADC3; // ADC Top - 10 wedges on top
    private int _TDC1; // TDC Front
    private int _TDC2; // TDC Back
    private int _TDC3; // TDC top - 10 wedges on top

    public Hit(int id, int sector, int superlayer, int layer, int paddle,
               int adc1, int adc2, int adc3, int tdc1, int tdc2, int tdc3)
    {
        _Id = id;
        _Sector = sector;
        _Superlayer = superlayer;
        _Layer = layer;
        _Paddle = paddle;
        _ADC1 = adc1;
        _ADC2 = adc2;
        _ADC3 = adc3;
        _TDC1 = tdc1;
        _TDC2 = tdc2;
        _TDC3 = tdc3;
    }
    // Define getters / setters
    public int get_Id() {
        return _Id;
    }

    public void set_Id(int _Id) {
        this._Id = _Id;
    }


    public int get_Sector() {
        return _Sector;
    }

    public void set_Sector(int _Sector) {
        this._Sector = _Sector;
    }

    public int get_Superlayer() {
        return _Superlayer;
    }

    public void set_Superlayer(int _Superlayer) {
        this._Superlayer = _Superlayer;
    }

    public int get_Layer() {
        return _Layer;
    }

    public void set_Layer(int _Layer) {
        this._Layer = _Layer;
    }

    public int get_Paddle() {
        return _Paddle;
    }

    public void set_Paddle(int _Paddle) {
        this._Paddle = _Paddle;
    }

    public int get_ADC1() {
        return _ADC1;
    }

    public void set_ADC1(int _ADC1) {
        this._ADC1 = _ADC1;
    }

    public int get_ADC2() {
        return _ADC2;
    }

    public void set_ADC2(int _ADC2) {
        this._ADC2 = _ADC2;
    }


    public int get_ADC3() {
        return _ADC3;
    }

    public void set_ADC3(int _ADC3) {
        this._ADC3 = _ADC3;
    }

    public int get_TDC1() {
        return _TDC1;
    }

    public void set_TDC1(int _TDC1) {
        this._TDC1 = _TDC1;
    }

    public int get_TDC2() {
        return _TDC2;
    }

    public void set_TDC2(int _TDC2) {
        this._TDC2 = _TDC2;
    }

    public int get_TDC3() {
        return _TDC3;
    }

    public void set_TDC3(int _TDC3) {
        this._TDC3 = _TDC3;
    }



    public void setPaddleLine(ALERTGeant4Factory geometry) {
/*
org.jlab.detector.volume.Geant4Basic pad = geometry
                .getPaddle(get_Paddle());
        Line3d l = pad.getLineZ();
        Line3D paddleLine = new Line3D();

        paddleLine.set(l.origin().x, l.origin().y, l.origin().z, l.end().x,
                l.end().y, l.end().z);
        this.set_paddleLine(paddleLine);

        this.set_barthickness(geometry.getThickness(get_Paddle()));
 */

    }



    private Point3D calc_hitPosition() {
        Point3D hitPosition = new Point3D();
        //get_paddleLine() ---- must be included to **
        /*
        Point3D startpoint = this.get_paddleLine().origin();

        hitPosition.setX(startpoint.x());
        hitPosition.setY(startpoint.y());
        hitPosition.setZ(this.get_y());
       */
        return hitPosition;
    }




    }
