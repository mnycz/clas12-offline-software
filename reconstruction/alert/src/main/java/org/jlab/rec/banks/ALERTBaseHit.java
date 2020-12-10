package org.jlab.rec.banks;

public class ALERTBaseHit implements Comparable<ALERTBaseHit> {

    private int _Id;
    private int _Sector;
    private int _Layer;
    private int _Superlayer;
    public int ADC1 = -1;
    public int ADC2 = -1;
    public int ADC3 = -1;
    public int TDC1 = -1;
    public int TDC2 = -1;
    public int TDC3 = -1;

    public double ADCTime1 = -1;
    public int ADCpedestal1 = -1;
    public double ADCTime2 = -1;
    public int ADCpedestal2 = -1;
    public double ADCTime3 = -1;
    public int ADCpedestal3 = -1;

    public int ADCbankHitIdx1 = -1;
    public int ADCbankHitIdx2 = -1;
    public int ADCbankHitIdx3 = -1;

    public int TDCbankHitIdx1 = -1;
    public int TDCbankHitIdx2 = -1;
    public int TDCbankHitIdx3 = -1;

    public ALERTBaseHit(int sector, int layer, int superlayer) {
        _Sector = sector;
        _Layer = layer;
        _Superlayer = superlayer;
    }

    @Override
    public int hashCode() {
        int hc = this._Sector * 10000 + this._Layer * 1000 + this._Superlayer;
        return hc;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ALERTBaseHit) {
            ALERTBaseHit hit = (ALERTBaseHit) obj;
            return (hit._Sector == this._Sector && hit._Layer == this._Layer
                    && hit._Superlayer == this._Superlayer
                    && hit.ADC1 == this.ADC1 && hit.ADC2 == this.ADC2&&hit.ADC3 == this.ADC3
                    && hit.TDC1 == this.TDC1 && hit.TDC2 == this.TDC2&&hit.TDC3 == this.TDC3
                    && hit.ADCTime1 == this.ADCTime1&& hit.ADCpedestal1 == this.ADCpedestal1
                    && hit.ADCTime2 == this.ADCTime2 && hit.ADCpedestal2 == this.ADCpedestal2
                    && hit.ADCTime3 == this.ADCTime3 && hit.ADCpedestal3 == this.ADCpedestal3);

        } else {
            return false;
        }
    }

    @Override
    public int compareTo(ALERTBaseHit arg) {
        // int return_val = 0 ;
        int CompSec = this._Sector < arg._Sector ? -1
                : this._Sector == arg._Sector ? 0 : 1;
        int CompLay = this._Layer < arg._Layer ? -1
                : this._Layer == arg._Layer ? 0 : 1;
        int CompId = this._Superlayer < arg._Superlayer ? -1
                : this._Superlayer == arg._Superlayer ? 0 : 1;

        int adc_hit = -1;
        int adc_arg = -1;

        int adcI_hit = -1;
        int adcI_arg = -1;

        if (this.ADC1 != -1) {
            adc_hit = this.ADC1;
            adcI_hit = this.ADCbankHitIdx1;
        }
        if (this.ADC2 != -1) {
            adc_hit = this.ADC2;
            adcI_hit = this.ADCbankHitIdx2;
        }
        if (this.ADC3 != -1) {
            adc_hit = this.ADC3;
            adcI_hit = this.ADCbankHitIdx3;
        }
        if (arg.ADC1 != -1) {
            adc_arg = arg.ADC1;
            adcI_arg = arg.ADCbankHitIdx1;
        }
        if (arg.ADC2 != -1) {
            adc_arg = arg.ADC2;
            adcI_arg = arg.ADCbankHitIdx2;
        }
        if (arg.ADC3 != -1) {
            adc_arg = arg.ADC3;
            adcI_arg = arg.ADCbankHitIdx3;
        }

        int tdc_hit = -1;
        int tdc_arg = -1;

        int tdcI_hit = -1;
        int tdcI_arg = -1;
        if (this.TDC1 != -1) {
            tdc_hit = this.TDC1;
            tdcI_hit = this.TDCbankHitIdx1;
        }
        if (this.TDC2 != -1) {
            tdc_hit = this.TDC2;
            tdcI_hit = this.TDCbankHitIdx2;
        }
        if (this.TDC3 != -1) {
            tdc_hit = this.TDC3;
            tdcI_hit = this.TDCbankHitIdx3;
        }
        if (arg.TDC1 != -1) {
            tdc_arg = arg.TDC1;
            tdcI_arg = arg.TDCbankHitIdx1;
        }
        if (this.TDC2 != -1) {
            tdc_arg = arg.TDC2;
            tdcI_arg = arg.TDCbankHitIdx2;
        }
        if (this.TDC3 != -1) {
            tdc_arg = arg.TDC3;
            tdcI_arg = arg.TDCbankHitIdx3;
        }
        int CompADC = this.ADC1 + this.ADC2 + this.ADC3 < arg.ADC1 + arg.ADC2 + arg.ADC3 ? -1 : this.ADC1 + this.ADC2 + this.ADC3 == arg.ADC1 + arg.ADC2 + arg.ADC3 ? 0 : 1;
        int CompTDC = this.TDC1 + this.TDC2 + this.TDC3 < arg.TDC1 + arg.TDC2 + arg.TDC3 ? -1 : this.TDC1 + this.TDC2 + this.TDC3 == arg.TDC1 + arg.TDC2 + arg.TDC3 ? 0 : 1;

        return ((CompTDC == 0) ? CompADC : CompTDC);
    }
    public int get_Sector() {
        return this._Sector;
    }

    public int get_Layer() {
        return this._Layer;
    }

    public int get_Superlayer() {
        return this._Superlayer;
    }

    public int get_ADC1() {
        return this.ADC1;
    }

    public int get_ADC2() {
        return this.ADC2;
    }
    public int get_ADC3() {
        return this.ADC3;
    }

    public int get_TDC1() {
        return this.TDC1;
    }

    public int get_TDC2() {
        return this.TDC2;
    }
    public int get_TDC3() {
        return this.TDC3;
    }

    public double get_ADCTime1() {
        return this.ADCTime1;
    }

    public int get_ADCpedestal1() {
        return this.ADCpedestal1;
    }
    public double get_ADCTime2() {
        return this.ADCTime2;
    }

    public int get_ADCpedestal2() {
        return this.ADCpedestal2;
    }

    public double get_ADCTime3() {
        return this.ADCTime3;
    }

    public int get_ADCpedestal3() {
        return this.ADCpedestal3;
    }

    public int get_Id() {
        return _Id;
    }

    public void set_Id(int _Id) {
        this._Id = _Id;
    }

}
