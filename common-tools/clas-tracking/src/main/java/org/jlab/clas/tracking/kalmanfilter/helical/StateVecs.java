package org.jlab.clas.tracking.kalmanfilter.helical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.geom.prim.Vector3D;

import Jama.Matrix;
import org.jlab.clas.swimtools.Swim;
import org.jlab.clas.tracking.Constants;
import org.jlab.clas.tracking.kalmanfilter.helical.MeasVecs.MeasVec;
import org.jlab.clas.tracking.trackrep.Helix;
import org.jlab.geom.prim.Point3D;

public class StateVecs {

    public List<B> bfieldPoints = new ArrayList<B>();
    public Map<Integer, StateVec> trackTraj = new HashMap<Integer, StateVec>();
    public Map<Integer, CovMat> trackCov = new HashMap<Integer, CovMat>();

    public StateVec StateVec;
    public CovMat CovMat;
    public Matrix F;

    public List<Double> X0;
    public List<Double> Y0;
    public List<Double> Z0; // reference points

    public List<Integer> Layer;
    public List<Integer> Sector;

    double[] value = new double[4]; // x,y,z,phi
    double[] swimPars = new double[7];
    public double[] getStateVecPosAtMeasSite(int k, StateVec kVec, MeasVec mv, Swim swim) {
        this.resetArrays(swimPars);
        this.resetArrays(value);
        double x0 = X0.get(k) + kVec.d_rho * Math.cos(kVec.phi0) ;
        double y0 = Y0.get(k) + kVec.d_rho * Math.sin(kVec.phi0) ;
        double z0 = Z0.get(k) + kVec.dz ;
        double invKappa = 1. / Math.abs(kVec.kappa);
        double px0 = -invKappa * Math.sin(kVec.phi0 );
        double py0 = invKappa * Math.cos(kVec.phi0 );
        double pz0 = invKappa * kVec.tanL;
        int ch = (int) KFitter.polarity*(int) Math.signum(kVec.kappa);
        
        if(mv.surface!=null) {
            swim.SetSwimParameters(
                    x0/Constants.getUnitScale(), 
                    y0/Constants.getUnitScale(),
                    z0/Constants.getUnitScale(), 
                    px0, py0, pz0, ch);
            
            if(mv.surface.plane!=null) {
                swimPars = swim.SwimToPlaneBoundary(mv.surface.plane.point().toVector3D().dot(mv.surface.plane.normal())/Constants.getUnitScale(), 
                        mv.surface.plane.normal(), 1);
            }
            if(mv.surface.cylinder!=null) {
                double r = 0.5*(mv.surface.cylinder.baseArc().radius()+mv.surface.cylinder.highArc().radius())/Constants.getUnitScale();
                swimPars = swim.SwimToCylinder(r); 
            }
            for(int j =0; j < 3; j++) {
                swimPars[j]*=Constants.getUnitScale();
            }
            double xc = X0.get(k) + (kVec.d_rho + kVec.alpha / kVec.kappa) * Math.cos(kVec.phi0);
            double yc = Y0.get(k) + (kVec.d_rho + kVec.alpha / kVec.kappa) * Math.sin(kVec.phi0);
            double r = Math.abs(kVec.alpha / kVec.kappa);
            Vector3D ToPoint = new Vector3D();
            Vector3D ToRef = new Vector3D(X0.get(k) - xc, Y0.get(k) - yc, 0);
            
            ToPoint = new Vector3D(swimPars[0] - xc, swimPars[1] - yc, 0);
            double phi = ToRef.angle(ToPoint);
            phi *= -Math.signum(kVec.kappa);
            
            value[0] = swimPars[0];
            value[1] = swimPars[1];
            value[2] = swimPars[2];
            value[3] = -(swimPars[2]-Z0.get(k) - kVec.dz)/(kVec.alpha / kVec.kappa * kVec.tanL) ;
//            phi = value[3];
//            double x = X0.get(k) + kVec.d_rho * Math.cos(kVec.phi0) + kVec.alpha / kVec.kappa * (Math.cos(kVec.phi0) - Math.cos(kVec.phi0 + phi));
//            double y = Y0.get(k) + kVec.d_rho * Math.sin(kVec.phi0) + kVec.alpha / kVec.kappa * (Math.sin(kVec.phi0) - Math.sin(kVec.phi0 + phi));
//            double z = Z0.get(k) + kVec.dz - kVec.alpha / kVec.kappa * kVec.tanL * phi;
//
        }
        return value;
    }

    public void setStateVecPosAtMeasSite(int k, StateVec kVec, MeasVec mv, Swim swimmer) {
        
        double[] pars = this.getStateVecPosAtMeasSite(k, kVec, mv, swimmer);
        if (pars == null) {
            return;
        }
        //System.out.println(" k "+k+" "+pars[0]+", "+pars[1]);
        kVec.x = pars[0];
        kVec.y = pars[1];
        kVec.z = pars[2];

        kVec.alpha = new B(k, kVec.x, kVec.y, kVec.z, swimmer).alpha;
        kVec.phi = pars[3];
    }

    public StateVec newStateVecAtMeasSite(int k, StateVec kVec, MeasVec mv, Swim swimmer) {

        StateVec newVec = kVec;
        double[] pars = this.getStateVecPosAtMeasSite(k, kVec, mv, swimmer);
        if (pars == null) {
            return null;
        }

        newVec.x = pars[0];
        newVec.y = pars[1];
        newVec.z = pars[2];

        newVec.alpha = new B(k, newVec.x, newVec.y, newVec.z, swimmer).alpha;
        newVec.phi = pars[3];

        // new state: 
        return newVec;
    }

    public StateVec transported(int i, int f, StateVec iVec, MeasVec mv, 
            Swim swimmer) { // s = signed step-size
        if (iVec.phi0 < 0) {
            iVec.phi0 += 2. * Math.PI;
        }

        B Bf = new B(i, iVec.x, iVec.y, iVec.z, swimmer);

        double Xc = X0.get(i) + (iVec.d_rho + iVec.alpha / iVec.kappa) * Math.cos(iVec.phi0);
        double Yc = Y0.get(i) + (iVec.d_rho + iVec.alpha / iVec.kappa) * Math.sin(iVec.phi0);

        // transport stateVec...
        StateVec fVec = new StateVec(f);

        double phi_f = Math.atan2(Yc - Y0.get(f), Xc - X0.get(f));
        if (iVec.kappa < 0) {
            phi_f = Math.atan2(-Yc + Y0.get(f), -Xc + X0.get(f));
        }

        if (phi_f < 0) {
            phi_f += 2 * Math.PI;
        }
        fVec.phi0 = phi_f;

        fVec.d_rho = (Xc - X0.get(f)) * Math.cos(phi_f) + (Yc - Y0.get(f)) * Math.sin(phi_f) - Bf.alpha / iVec.kappa;

        fVec.kappa = iVec.kappa;

        double[] ElossTot = ELoss_hypo(iVec, f - i);
        for (int e = 0; e < 3; e++) {
            ElossTot[e] = iVec.get_ELoss()[e] + ElossTot[e];
        }
        fVec.set_ELoss(ElossTot);

        fVec.dz = Z0.get(i) - Z0.get(f) + iVec.dz - (Bf.alpha / iVec.kappa) * (phi_f - iVec.phi0) * iVec.tanL;

        fVec.tanL = iVec.tanL;

        //Bf = new B(f, X0.get(f), Y0.get(f), Z0.get(f));
        fVec.alpha = Bf.alpha;

        int dir = (int) Math.signum(f-i);
        ////System.out.println("... B "+Bf.Bz+"Z0.get(i)"+ Z0.get(i) +" Z0.get(f) "+Z0.get(f));
        this.newStateVecAtMeasSite(f, fVec, mv, swimmer);

        return fVec;
        
    }
    
    public void transport(int i, int f, StateVec iVec, CovMat icovMat, MeasVec mv, 
            Swim swimmer) { // s = signed step-size
        if (iVec.phi0 < 0) {
            iVec.phi0 += 2. * Math.PI;
        }

        B Bf = new B(i, iVec.x, iVec.y, iVec.z, swimmer);

        double Xc = X0.get(i) + (iVec.d_rho + iVec.alpha / iVec.kappa) * Math.cos(iVec.phi0);
        double Yc = Y0.get(i) + (iVec.d_rho + iVec.alpha / iVec.kappa) * Math.sin(iVec.phi0);
        
        // transport stateVec...
        StateVec fVec = new StateVec(f);

        double phi_f = Math.atan2(Yc - Y0.get(f), Xc - X0.get(f));
        if (iVec.kappa < 0) {
            phi_f = Math.atan2(-Yc + Y0.get(f), -Xc + X0.get(f));
        }

        if (phi_f < 0) {
            phi_f += 2 * Math.PI;
        }
        fVec.phi0 = phi_f;

        fVec.d_rho = (Xc - X0.get(f)) * Math.cos(phi_f) + (Yc - Y0.get(f)) * Math.sin(phi_f) - Bf.alpha / iVec.kappa;

        fVec.kappa = iVec.kappa;

        double[] ElossTot = ELoss_hypo(iVec, f - i);
        for (int e = 0; e < 3; e++) {
            ElossTot[e] = iVec.get_ELoss()[e] + ElossTot[e];
        }
        fVec.set_ELoss(ElossTot);

        fVec.dz = Z0.get(i) - Z0.get(f) + iVec.dz - (Bf.alpha / iVec.kappa) * (phi_f - iVec.phi0) * iVec.tanL;

        fVec.tanL = iVec.tanL;

        //Bf = new B(f, X0.get(f), Y0.get(f), Z0.get(f));
        fVec.alpha = Bf.alpha;
        
        int dir = (int) Math.signum(f-i);
        ////System.out.println("... B "+Bf.Bz+"Z0.get(i)"+ Z0.get(i) +" Z0.get(f) "+Z0.get(f));
        this.newStateVecAtMeasSite(f, fVec, mv, swimmer);

        // now transport covMat...
        double dphi0_prm_del_drho = -1. / (fVec.d_rho + iVec.alpha / iVec.kappa) * Math.sin(fVec.phi0 - iVec.phi0);
        double dphi0_prm_del_phi0 = (iVec.d_rho + iVec.alpha / iVec.kappa) / (fVec.d_rho + iVec.alpha / iVec.kappa) * Math.cos(fVec.phi0 - iVec.phi0);
        double dphi0_prm_del_kappa = (iVec.alpha / (iVec.kappa * iVec.kappa)) / (fVec.d_rho + iVec.alpha / iVec.kappa) * Math.sin(fVec.phi0 - iVec.phi0);
        double dphi0_prm_del_dz = 0;
        double dphi0_prm_del_tanL = 0;

        double drho_prm_del_drho = Math.cos(fVec.phi0 - iVec.phi0);
        double drho_prm_del_phi0 = (iVec.d_rho + iVec.alpha / iVec.kappa) * Math.sin(fVec.phi0 - iVec.phi0);
        double drho_prm_del_kappa = (iVec.alpha / (iVec.kappa * iVec.kappa)) * (1 - Math.cos(fVec.phi0 - iVec.phi0));
        double drho_prm_del_dz = 0;
        double drho_prm_del_tanL = 0;

        double dkappa_prm_del_drho = 0;
        double dkappa_prm_del_phi0 = 0;
        double dkappa_prm_del_dkappa = 1;
        double dkappa_prm_del_dz = 0;
        double dkappa_prm_del_tanL = 0;

        double dz_prm_del_drho = ((iVec.alpha / iVec.kappa) / (fVec.dz + iVec.alpha / iVec.kappa)) * iVec.tanL * Math.sin(fVec.phi0 - iVec.phi0);
        double dz_prm_del_phi0 = (iVec.alpha / iVec.kappa) * iVec.tanL * (1 - Math.cos(fVec.phi0 - iVec.phi0) * (iVec.dz + iVec.alpha / iVec.kappa) / (fVec.dz + iVec.alpha / iVec.kappa));
        double dz_prm_del_kappa = (iVec.alpha / (iVec.kappa * iVec.kappa)) * iVec.tanL * (fVec.phi0 - iVec.phi0 - Math.sin(fVec.phi0 - iVec.phi0) * (iVec.alpha / iVec.kappa) / (fVec.dz + iVec.alpha / iVec.kappa));
        double dz_prm_del_dz = 1;
        double dz_prm_del_tanL = -iVec.alpha * (fVec.phi0 - iVec.phi0) / iVec.kappa;

        double dtanL_prm_del_drho = 0;
        double dtanL_prm_del_phi0 = 0;
        double dtanL_prm_del_dkappa = 0;
        double dtanL_prm_del_dz = 0;
        double dtanL_prm_del_tanL = 1;

        double[][] FMat = new double[][]{
            {drho_prm_del_drho, drho_prm_del_phi0, drho_prm_del_kappa, drho_prm_del_dz, drho_prm_del_tanL},
            {dphi0_prm_del_drho, dphi0_prm_del_phi0, dphi0_prm_del_kappa, dphi0_prm_del_dz, dphi0_prm_del_tanL},
            {dkappa_prm_del_drho, dkappa_prm_del_phi0, dkappa_prm_del_dkappa, dkappa_prm_del_dz, dkappa_prm_del_tanL},
            {dz_prm_del_drho, dz_prm_del_phi0, dz_prm_del_kappa, dz_prm_del_dz, dz_prm_del_tanL},
            {dtanL_prm_del_drho, dtanL_prm_del_phi0, dtanL_prm_del_dkappa, dtanL_prm_del_dz, dtanL_prm_del_tanL}
        };

        //StateVec = fVec;
        this.trackTraj.put(f, fVec);
        F = new Matrix(FMat);
        Matrix FT = F.transpose();
        Matrix Cpropagated = FT.times(icovMat.covMat).times(F);
        if (Cpropagated != null) {
            CovMat fCov = new CovMat(f);
            fCov.covMat = Cpropagated.plus(this.Q(iVec, f - i));
            //CovMat = fCov;
            this.trackCov.put(f, fCov);
        }
    }
    private double get_t_ov_X0(double radius) {
        double value = 0;
        return value;
    }
    
    private double detMat_Z_ov_A_timesThickn(double radius) {    
        double value = 0;
        
        return value;
    }
    
    private double[] ELoss_hypo(StateVec iVec, int dir) {
        double[] Eloss = new double[3]; 

        Vector3D trkDir = this.P(iVec.k);
        trkDir.unit();
        double cosEntranceAngle = trkDir.z();
       // System.out.println(" cosTrk "+Math.toDegrees(Math.acos(trkDir.z()))+" at state "+iVec.k+" dir "+dir);
        double pt = Math.abs(1. / iVec.kappa);
        double pz = pt * iVec.tanL;
        double p = Math.sqrt(pt * pt + pz * pz);

        for (int hyp = 2; hyp < 5; hyp++) {

            double mass = MassHypothesis(hyp); // assume given mass hypothesis
            double beta = p / Math.sqrt(p * p + mass * mass); // use particle momentum
            double gamma = 1. / Math.sqrt(1 - beta * beta);

            double s = MassHypothesis(1) / mass;

            double Wmax = 2. * mass * beta * beta * gamma * gamma / (1. + 2. * s * gamma + s * s);
            double I = 0.000000172;

            double logterm = 2. * mass * beta * beta * gamma * gamma * Wmax / (I * I);

            double delta = 0.;
            double dEdx = 0.00001535 * this.detMat_Z_ov_A_timesThickn(Math.sqrt(iVec.x*iVec.x+iVec.y*iVec.y)) * (Math.log(logterm) - 2 * beta * beta - delta) / (beta * beta); //in GeV/mm
            //System.out.println(" mass hy "+hyp+" Mat at "+Math.sqrt(iVec.x*iVec.x+iVec.y*iVec.y)+"Z/A*t "+this.detMat_Z_ov_A_timesThickn(Math.sqrt(iVec.x*iVec.x+iVec.y*iVec.y))+" dEdx "+dEdx);
            Eloss[hyp - 2] = dir * Math.abs(dEdx / cosEntranceAngle);
        }
        return Eloss;
    }

    private Matrix Q(StateVec iVec, int dir) {

        Matrix Q = new Matrix(new double[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0}
        });

        // if (iVec.k % 2 == 1 && dir > 0) {
        if (dir >99999990 ) {
            Vector3D trkDir = this.P(iVec.k);
            trkDir.unit();
            double cosEntranceAngle = Math.abs(this.P(iVec.k).z());

            double pt = Math.abs(1. / iVec.kappa);
            double pz = pt * iVec.tanL;
            double p = Math.sqrt(pt * pt + pz * pz);

            //double t_ov_X0 = 2. * 0.32 / Constants.SILICONRADLEN; //path length in radiation length units = t/X0 [true path length/ X0] ; Si radiation length = 9.36 cm
            double t_ov_X0 = this.get_t_ov_X0(Math.sqrt(iVec.x*iVec.x+iVec.y*iVec.y)); //System.out.println(Math.log(t_ov_X0)/9.+" rad "+Math.sqrt(iVec.x*iVec.x+iVec.y*iVec.y)+" t/x0 "+t_ov_X0);
            double mass = MassHypothesis(2);   // assume given mass hypothesis (2=pion)
            double beta = p / Math.sqrt(p * p + mass * mass); // use particle momentum
            double pathLength = t_ov_X0 / cosEntranceAngle;
            double sctRMS = (0.00141 / (beta * p)) * Math.sqrt(pathLength) * (1 + Math.log10(pathLength)/9.); // Highland-Lynch-Dahl formula
            
            Q = new Matrix(new double[][]{
                {0, 0, 0, 0, 0},
                {0, sctRMS*sctRMS * (1 + iVec.tanL * iVec.tanL), 0, 0, 0},
                {0, 0, sctRMS*sctRMS * (iVec.kappa * iVec.kappa * iVec.tanL * iVec.tanL), 0, sctRMS*sctRMS * (iVec.kappa * iVec.tanL * (1 + iVec.tanL * iVec.tanL))},
                {0, 0, 0, 0, 0},
                {0, 0, sctRMS*sctRMS * (iVec.kappa * iVec.tanL * (1 + iVec.tanL * iVec.tanL)), 0, sctRMS*sctRMS * (1 + iVec.tanL * iVec.tanL) * (1 + iVec.tanL * iVec.tanL)}
            });
        }

        return Q;

    }
    private StateVec reset(StateVec SV, StateVec stateVec) {
        SV = new StateVec(stateVec.k);
        SV.x = stateVec.x;
        SV.y = stateVec.y;
        SV.z = stateVec.z;
        SV.d_rho = stateVec.d_rho;
        SV.dz = stateVec.dz;
        SV.phi0 = stateVec.phi0;
        SV.phi = stateVec.phi;
        SV.tanL = stateVec.tanL;
        SV.alpha = stateVec.alpha;
        SV.kappa = stateVec.kappa;

        return SV;
    }
    private double[] delta_d_a = new double[] {1, Math.toRadians(0.25),  0.01, 1, 0.01};
    private double[] diagElmts = new double[] {0.1, Math.toRadians(0.15), 1./0.05, 0.1, Math.toRadians(0.25)};
    double[][] Ci(StateVec stateVec, MeasVec mv, 
            Swim swimmer) {
        double[][] _F = new double[5][5];
        double[][] _C = new double[5][5];
        double[][] C = new double[5][5];
        StateVec SVplus = null;
        StateVec SVminus = null;

        for(int i = 0; i < delta_d_a.length; i++) {
            SVplus = this.reset(SVplus, stateVec);
            SVminus = this.reset(SVminus, stateVec);
            if(i ==0) {
                SVplus.d_rho = stateVec.d_rho + delta_d_a[i] / 2.;
                SVminus.d_rho = stateVec.d_rho - delta_d_a[i] / 2.;
            }
            if(i ==1) {
                SVplus.phi0 = stateVec.phi0 + delta_d_a[i] / 2.;
                SVminus.phi0 = stateVec.phi0 - delta_d_a[i] / 2.;
            }
            if(i ==2) {
                SVplus.kappa = stateVec.kappa + delta_d_a[i] / 2.;
                SVminus.kappa = stateVec.kappa - delta_d_a[i] / 2.;
            }
            if(i ==3) {
                SVplus.z = stateVec.z + delta_d_a[i] / 2.;
                SVminus.z = stateVec.z - delta_d_a[i] / 2.;
            }
            if(i ==4) {
                SVplus.tanL = stateVec.tanL + delta_d_a[i] / 2.;
                SVminus.tanL = stateVec.tanL - delta_d_a[i] / 2.;
            }
            SVplus = this.transported(0, 1, SVplus, mv, 
                        swimmer);
            SVminus = this.transported(0, 1, SVminus, mv, 
                        swimmer);
           
            _F[i][0] = (SVplus.d_rho - SVminus.d_rho)/delta_d_a[i];
            _F[i][1] = (SVplus.phi0 - SVminus.phi0)/delta_d_a[i];
            _F[i][2] = (SVplus.kappa - SVminus.kappa)/delta_d_a[i];
            _F[i][3] = (SVplus.z - SVminus.z)/delta_d_a[i];
            _F[i][4] = (SVplus.tanL - SVminus.tanL)/delta_d_a[i];
        }
        
        for(int i = 0; i<5; i++) {
            for(int j = 0; j<5; j++) {
                _C[i][j] = diagElmts[i]*diagElmts[i]*_F[i][j];
            }
        }
        for(int i = 0; i<5; i++) {
            for(int j = 0; j<5; j++) {
                C[i][j] += _C[j][i]*_F[i][j];
            }
        }
        
        //for(int j = 0; j<5; j++) {
        //    C[j][j] += diagElmts[j]*diagElmts[j];
        //}
        
        return C;
        
    }
    private Matrix initCovMat(StateVec stateVec, MeasVec mv, 
            Swim swimmer) {
        //org.jlab.jnp.matrix.Matrix initCMatrix = new org.jlab.jnp.matrix.Matrix(); 
        return new Matrix(this.Ci(stateVec, mv, swimmer));
    }

    private void resetArrays(double[] swimPars) {
        for(int i = 0; i<swimPars.length; i++) {
            swimPars[i] = 0;
        }
    }

    public class StateVec {

        final int k;

        public double x;
        public double y;
        public double z;
        public double kappa;
        public double d_rho;
        public double phi0;
        public double phi;
        public double tanL;
        public double dz;
        public double alpha;

        StateVec(int k) {
            this.k = k;
        }
        private double[] _ELoss = new double[3];

        public double[] get_ELoss() {
            return _ELoss;
        }

        public void set_ELoss(double[] _ELoss) {
            this._ELoss = _ELoss;
        }

    }

    public class CovMat {

        final int k;
        public Matrix covMat;

        CovMat(int k) {
            this.k = k;
        }

    }

    public class B {

        final int k;
        double x;
        double y;
        double z;
        Swim swimmer;
        
        public double Bx;
        public double By;
        public double Bz;

        public double alpha;

        float b[] = new float[3];
        B(int k, double x, double y, double z, Swim swimmer) {
            this.k = k;
            this.x = x;
            this.y = y;
            this.z = z;

            swimmer.BfieldLab(x/Constants.getUnitScale(), y/Constants.getUnitScale(), (z+Z0.get(0))/Constants.getUnitScale(), b);
            this.Bx = b[0];
            this.By = b[1];
            this.Bz = b[2];

            this.alpha = 1. / (Constants.getLIGHTVEL() * Math.sqrt(b[0]*b[0]+b[1]*b[1]+b[2]*b[2]));
            //this.alpha = 1. / (5.);
        }
    }

    //public String massHypo = "pion";
    public double MassHypothesis(int H) {
        double piMass = 0.13957018;
        double KMass = 0.493677;
        double muMass = 0.105658369;
        double eMass = 0.000510998;
        double pMass = 0.938272029;
        double value = piMass; //default
        if (H == 4) {
            value = pMass;
        }
        if (H == 1) {
            value = eMass;
        }
        if (H == 2) {
            value = piMass;
        }
        if (H == 3) {
            value = KMass;
        }
        if (H == 0) {
            value = muMass;
        }
        return value;
    }

    public Vector3D P(int kf) {
        if (this.trackTraj.get(kf) != null) {
            //double x = this.trackTraj.get(kf).x;
            //double y = this.trackTraj.get(kf).y;
            //double z = this.trackTraj.get(kf).z; 
            //B Bf = new B(kf, x, y, z);
            double px = -Math.signum(1 / this.trackTraj.get(kf).kappa) * Math.sin(this.trackTraj.get(kf).phi0 + this.trackTraj.get(kf).phi);
            double py = Math.signum(1 / this.trackTraj.get(kf).kappa) * Math.cos(this.trackTraj.get(kf).phi0 + this.trackTraj.get(kf).phi);
            double pz = Math.signum(1 / this.trackTraj.get(kf).kappa) * this.trackTraj.get(kf).tanL;
            //int q = (int) Math.signum(this.trackTraj.get(kf).kappa);

            return new Vector3D(px, py, pz);
        } else {
            return new Vector3D(0, 0, 0);
        }

    }

    public Helix setTrackPars(int kf) {

        double x = this.trackTraj.get(kf).d_rho * Math.cos(this.trackTraj.get(kf).phi0);
        double y = this.trackTraj.get(kf).d_rho * Math.sin(this.trackTraj.get(kf).phi0);
        double z = this.trackTraj.get(kf).dz;
        double px = -Math.abs(1. / this.trackTraj.get(kf).kappa) * Math.sin(this.trackTraj.get(kf).phi0);
        double py = Math.abs(1. / this.trackTraj.get(kf).kappa) * Math.cos(this.trackTraj.get(kf).phi0);
        double pz = Math.abs(1. / this.trackTraj.get(kf).kappa) * this.trackTraj.get(kf).tanL;
        int q = (int) Math.signum(this.trackTraj.get(kf).kappa);
        double p_unc = Math.sqrt(px * px + py * py + pz * pz);

        double E_loss = this.trackTraj.get(kf).get_ELoss()[2];
        double B = 1./this.trackTraj.get(0).alpha/Constants.getLIGHTVEL() ;
        double h_dca = Math.sqrt(x * x + y * y);
        double h_phi0 = Math.atan2(py, px);
        if(Math.abs(Math.sin(h_phi0))>0.1) {
            h_dca = -x/Math.sin(h_phi0);
        } else {
            h_dca = y/Math.cos(h_phi0);
        }
            
        double kappa = Math.signum(this.trackTraj.get(kf).kappa) / Math.sqrt(px * px + py * py);
        double h_omega = kappa/this.trackTraj.get(0).alpha;
        double h_dz = z;
        double h_tandip = pz / Math.sqrt(px * px + py * py);
        
        Helix trkHelix = new Helix(x, y, x, px, py, pz, q, B);
        
        return trkHelix;
    }

    public void init(Helix trk, Matrix cov, double Xb, double Yb, KFitter kf, MeasVec mv, 
            Swim swimmer) {
        //init stateVec
        StateVec initSV = new StateVec(0);
        initSV.x = trk.getX();
        initSV.y = trk.getY();
        initSV.z = trk.getZ();
        double xcen = trk.getXc();
        double ycen = trk.getYc();
        B Bf = new B(0, (float)initSV.x, (float)initSV.x, (float)initSV.z, swimmer);
        initSV.alpha = Bf.alpha;
        initSV.kappa = Bf.alpha * trk.getOmega();
        initSV.phi0 = Math.atan2(ycen, xcen);
        if (initSV.kappa < 0) {
            initSV.phi0 = Math.atan2(-ycen, -xcen);
        }
        initSV.dz    = trk.getZ();
        initSV.tanL  = trk.getTanL();
        initSV.d_rho = trk.getD0();
        initSV.phi = 0;
        
        this.trackTraj.put(0, initSV);
        
        CovMat initCM = new CovMat(0);
        initCM.covMat = cov;
        this.trackCov.put(0, initCM);
    }

    public void printMatrix(Matrix C) {
        for (int k = 0; k < 5; k++) {
            System.out.println(C.get(k, 0) + "	" + C.get(k, 1) + "	" + C.get(k, 2) + "	" + C.get(k, 3) + "	" + C.get(k, 4));
        }
    }

    public void printlnStateVec(StateVec S) {
        System.out.println(S.k + ") drho " + S.d_rho + " phi0 " + S.phi0 + " kappa " + S.kappa + " dz " + S.dz + " tanL " + S.tanL + " phi " + S.phi + " x " + S.x + " y " + S.y + " z " + S.z + " alpha " + S.alpha);
    }
}
