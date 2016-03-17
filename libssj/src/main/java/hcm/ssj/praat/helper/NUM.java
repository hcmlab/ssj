/*
 * NUM.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.praat.helper;

/*
 * pb 2002/03/07 GPL
 * pb 2003/06/19 ridders3 replaced with ridders
 * pb 2003/07/09 gsl
 * pb 2003/08/27 NUMfisherQ: underflow and iteration excess should not return NUMundefined
 * pb 2005/07/08 NUMpow
 * pb 2006/08/02 NUMinvSigmoid
 * pb 2007/01/27 use final static doubles for value interpolation
 * pb 2007/08/20 built a "weird value" check into NUMviterbi (bug report by Adam Jacks)
 * pb 2008/01/19 double
 * pb 2008/09/21 NUMshift
 * pb 2008/09/22 NUMscale
 * pb 2011/03/29 C++
 */

/**
 * Created by Johnny on 03.06.2015.
 */
public class NUM
{
    
/********** Constants **********
 * Forty-digit constants computed by e.g.:
 *    bc -l
 *       scale=42
 *       print e(1)
 * Then rounding away the last two digits.
 */
    public final static double e = 2.7182818284590452353602874713526624977572;
    public final static double log2e = 1.4426950408889634073599246810018921374266;
    public final static double log2_10 = 3.3219280948873623478703194294893901758648;
    public final static double log10e = 0.4342944819032518276511289189166050822944;
    public final static double log10_2 = 0.3010299956639811952137388947244930267682;
    public final static double ln2 = 0.6931471805599453094172321214581765680755;
    public final static double ln10=  2.3025850929940456840179914546843642076011;
    public final static double pix2 = 6.2831853071795864769252867665590057683943;
    public final static double pi = 3.1415926535897932384626433832795028841972;
    public final static double pi_2 = 1.5707963267948966192313216916397514420986;
    public final static double pi_4 = 0.7853981633974483096156608458198757210493;
    public final static double _1_pi = 0.3183098861837906715377675267450287240689;
    public final static double _2_pi = 0.6366197723675813430755350534900574481378;
    public final static double sqrtpi = 1.7724538509055160272981674833411451827975;
    public final static double sqrt2pi = 2.5066282746310005024157652848110452530070;
    public final static double _1_sqrt2pi = 0.3989422804014326779399460599343818684759;
    public final static double _2_sqrtpi = 1.1283791670955125738961589031215451716881;
    public final static double lnpi = 1.1447298858494001741434273513530587116473;
    public final static double sqrt2 = 1.4142135623730950488016887242096980785697;
    public final static double sqrt1_2 = 0.7071067811865475244008443621048490392848;
    public final static double sqrt3 = 1.7320508075688772935274463415058723669428;
    public final static double sqrt5 = 2.2360679774997896964091736687312762354406;
    public final static double sqrt6 = 2.4494897427831780981972840747058913919659;
    public final static double sqrt7 = 2.6457513110645905905016157536392604257102;
    public final static double sqrt8 = 2.8284271247461900976033774484193961571393;
    public final static double sqrt10 = 3.1622776601683793319988935444327185337196;
    public final static double _goldenSection = 0.6180339887498948482045868343656381177203;

    // The Euler-Mascheroni constant cannot be computed by bc.
    // Instead we use the 40 digits computed by Johann von Soldner in 1809.
    public final static double _euler = 0.5772156649015328606065120900824024310422;
    public final static double undefined = Double.MAX_VALUE;

    public static double NUMpow(double base, double exponent) { return base <= 0.0 ? 0.0 : Math.pow(base, exponent); }

    public static void NUMshift(double x, double xfrom, double xto)
    {
        if (x == xfrom) x = xto;
        else x += xto - xfrom;
    }

    public static void NUMscale(double x, double xminfrom, double xmaxfrom, double xminto, double xmaxto)
    {
        if (x == xminfrom)x = xminto;
        else if (x == xmaxfrom)x = xmaxto;
        else x = xminto + (xmaxto - xminto) * ((x - xminfrom) / (xmaxfrom - xminfrom));
    }

//    void NUMinit(void)
//    {
//        gsl_set_error_handler_off();
//        NUMrandom_init();
//    }

//    void NUMfbtoa(double formant, double bandwidth, double dt, double a1, double a2)
//    {
//        a1 = 2 * Math.exp(-NUMpi * bandwidth * dt) * Math.cos(2 * NUMpi * formant * dt);
//        a2 = Math.exp(-2 * NUMpi * bandwidth * dt);
//    }
//
//    void NUMfilterSecondOrderSection_a(double x[], int n, double a1, double a2)
//    {
//        x[2] += a1 * x[1];
//        for (int i = 3; i <= n; i++)
//            x[i] += a1 * x[i - 1] - a2 * x[i - 2];
//    }
//
//    void NUMfilterSecondOrderSection_fb(double x[], int n, double dt, double formant, double bandwidth)
//    {
//        double a1, a2;
//        NUMfbtoa(formant, bandwidth, dt, & a1,&a2);
//        NUMfilterSecondOrderSection_a(x, n, a1, a2);
//    }
//
//    double NUMftopreemphasis(double f, double dt)
//    {
//        return Math.exp(-2.0 * NUMpi * f * dt);
//    }
//
//    void NUMpreemphasize_a(double x[], int n, double preemphasis)
//    {
//        for (int i = n; i >= 2; i--)
//            x[i] -= preemphasis * x[i - 1];
//    }
//
//    void NUMdeemphasize_a(double x[], int n, double preemphasis)
//    {
//        int i;
//        for (i = 2; i <= n; i++)
//            x[i] += preemphasis * x[i - 1];
//    }
//
//    void NUMpreemphasize_f(double x[], int n, double dt, double frequency)
//    {
//        NUMpreemphasize_a(x, n, NUMftopreemphasis(frequency, dt));
//    }
//
//    void NUMdeemphasize_f(double x[], int n, double dt, double frequency)
//    {
//        NUMdeemphasize_a(x, n, NUMftopreemphasis(frequency, dt));
//    }
//
//    void NUMautoscale(double x[], int n, double scale)
//    {
//        double maximum = 0.0;
//        for (int i = 1; i <= n; i++)
//            if (fMath.abs(x[i]) > maximum) maximum = fMath.abs(x[i]);
//        if (maximum > 0.0)
//        {
//            double factor = scale / maximum;
//            for (int i = 1; i <= n; i++)
//                x[i] *= factor;
//        }
//    }
//
//    double NUMlnGamma(double x)
//    {
//        gsl_sf_result result;
//        int status = gsl_sf_lngamma_e(x, & result);
//        return status == GSL_SUCCESS ? result.val : NUMundefined;
//    }
//
//    double NUMbeta(double z, double w)
//    {
//        if (z <= 0.0 || w <= 0.0) return NUMundefined;
//        return Math.exp(NUMlnGamma(z) + NUMlnGamma(w) - NUMlnGamma(z + w));
//    }
//
//    double NUMincompleteBeta(double a, double b, double x)
//    {
//        gsl_sf_result result;
//        int status = gsl_sf_beta_inc_e(a, b, x, & result);
//        if (status != GSL_SUCCESS && status != GSL_EUNDRFLW && status != GSL_EMAXITER)
//        {
//            Melder_fatal("NUMincompleteBeta status %d", status);
//            return NUMundefined;
//        }
//        return result.val;
//    }
//
//    double NUMbinomialP(double p, double k, double n)
//    {
//        double binomialQ;
//        if (p < 0.0 || p > 1.0 || n <= 0.0 || k < 0.0 || k > n) return NUMundefined;
//        if (k == n) return 1.0;
//        binomialQ = NUMincompleteBeta(k + 1, n - k, p);
//        if (binomialQ == NUMundefined) return NUMundefined;
//        return 1.0 - binomialQ;
//    }
//
//    double NUMbinomialQ(double p, double k, double n)
//    {
//        if (p < 0.0 || p > 1.0 || n <= 0.0 || k < 0.0 || k > n) return NUMundefined;
//        if (k == 0.0) return 1.0;
//        return NUMincompleteBeta(k, n - k + 1, p);
//    }
//
//    struct binomial
//
//    {
//        double p, k, n;
//    }
//
//    ;
//
//    static double binomialP(double p, void binomial_void)
//    {
//        struct binomial*binomial = (struct binomial *)binomial_void;
//        return NUMbinomialP(p, binomial -> k, binomial -> n) - binomial -> p;
//    }
//
//    static double binomialQ(double p, void binomial_void)
//    {
//        struct binomial*binomial = (struct binomial *)binomial_void;
//        return NUMbinomialQ(p, binomial -> k, binomial -> n) - binomial -> p;
//    }
//
//    double NUMinvBinomialP(double p, double k, double n)
//    {
//        static struct binomial binomial;
//        if (p < 0 || p > 1 || n <= 0 || k < 0 || k > n) return NUMundefined;
//        if (k == n) return 1.0;
//        binomial.p = p;
//        binomial.k = k;
//        binomial.n = n;
//        return NUMridders(binomialP, 0.0, 1.0, & binomial);
//    }
//
//    double NUMinvBinomialQ(double p, double k, double n)
//    {
//        static struct binomial binomial;
//        if (p < 0 || p > 1 || n <= 0 || k < 0 || k > n) return NUMundefined;
//        if (k == 0) return 0.0;
//        binomial.p = p;
//        binomial.k = k;
//        binomial.n = n;
//        return NUMridders(binomialQ, 0.0, 1.0, & binomial);
//    }

    /* Modified Bessel function I0. Abramowicz & Stegun, p. 378.*/
    public static double bessel_i0_f(double x)
    {
        if (x < 0.0) return bessel_i0_f(-x);
        if (x < 3.75)
        {
            /* Formula 9.8.1. Accuracy 1.6e-7. */
            double t = x / 3.75;
            t *= t;
            return 1.0 + t * (3.5156229 + t * (3.0899424 + t * (1.2067492
                    + t * (0.2659732 + t * (0.0360768 + t * 0.0045813)))));
        }
        /*
            otherwise: x >= 3.75
        */
        /* Formula 9.8.2. Accuracy of the polynomial factor 1.9e-7. */
        double t = 3.75 / x;   /* <= 1.0 */
        return Math.exp(x) / Math.sqrt(x) * (0.39894228 + t * (0.01328592
                + t * (0.00225319 + t * (-0.00157565 + t * (0.00916281
                + t * (-0.02057706 + t * (0.02635537 + t * (-0.01647633
                + t * 0.00392377))))))));
    }

    /* Modified Bessel function I1. Abramowicz & Stegun, p. 378. */
    public static double bessel_i1_f(double x)
    {
        if (x < 0.0) return -bessel_i1_f(-x);
        if (x < 3.75)
        {
            /* Formula 9.8.3. Accuracy of the polynomial factor 8e-9. */
            double t = x / 3.75;
            t *= t;
            return x * (0.5 + t * (0.87890594 + t * (0.51498869 + t * (0.15084934
                    + t * (0.02658733 + t * (0.00301532 + t * 0.00032411))))));
        }
        /*
            otherwise: x >= 3.75
        */
        /* Formula 9.8.4. Accuracy of the polynomial factor 2.2e-7. */
        double t = 3.75 / x;   /* <= 1.0 */
        return Math.exp(x) / Math.sqrt(x) * (0.39894228 + t * (-0.03988024
                + t * (-0.00362018 + t * (0.00163801 + t * (-0.01031555
                + t * (0.02282967 + t * (-0.02895312 + t * (0.01787654
                + t * (-0.00420059)))))))));
    }

//    double NUMbesselI(int n, double x)
//    {
//        gsl_sf_result result;
//        int status = gsl_sf_bessel_In_e(n, x, & result);
//        return status == GSL_SUCCESS ? result.val : NUMundefined;
//    }
//
//    /* Modified Bessel function K0. Abramowicz & Stegun, p. 379. */
//    double NUMbessel_k0_f(double x)
//    {
//        if (x <= 0.0) return NUMundefined;   /* Positive infinity. */
//        if (x <= 2.0)
//        {
//            /* Formula 9.8.5. Accuracy 1e-8. */
//            double x2 = 0.5 * x, t = x2 * x2;
//            return -Math.log(x2) * NUMbessel_i0_f(x) + (-0.57721566 + t * (0.42278420
//                    + t * (0.23069756 + t * (0.03488590 + t * (0.00262698
//                    + t * (0.00010750 + t * 0.00000740))))));
//        }
//        /*
//            otherwise: 2 < x < positive infinity
//        */
//        /* Formula 9.8.6. Accuracy of the polynomial factor 1.9e-7. */
//        double t = 2.0 / x;   /* < 1.0 */
//        return Math.exp(-x) / Math.sqrt(x) * (1.25331414 + t * (-0.07832358
//                + t * (0.02189568 + t * (-0.01062446 + t * (0.00587872
//                + t * (-0.00251540 + t * 0.00053208))))));
//    }
//
//    /* Modified Bessel function K1. Abramowicz & Stegun, p. 379. */
//    double NUMbessel_k1_f(double x)
//    {
//        if (x <= 0.0) return NUMundefined;   /* Positive infinity. */
//        if (x <= 2.0)
//        {
//            /* Formula 9.8.7. Accuracy  of the polynomial factor 8e-9. */
//            double x2 = 0.5 * x, t = x2 * x2;
//            return Math.log(x2) * NUMbessel_i1_f(x) + (1.0 / x) * (1.0 + t * (0.15443144
//                    + t * (-0.67278579 + t * (-0.18156897 + t * (-0.01919402
//                    + t * (-0.00110404 + t * (-0.00004686)))))));
//        }
//        /*
//            otherwise: 2 < x < positive infinity
//        */
//        /* Formula 9.8.8. Accuracy of the polynomial factor 2.2e-7. */
//        double t = 2.0 / x;   /* < 1.0 */
//        return Math.exp(-x) / Math.sqrt(x) * (1.25331414 + t * (0.23498619
//                + t * (-0.03655620 + t * (0.01504268 + t * (-0.00780353
//                + t * (0.00325614 + t * (-0.00068245)))))));
//    }
//
//    double NUMbesselK_f(int n, double x)
//    {
//        double twoByX, besselK_min2, besselK_min1, besselK = NUMundefined;
//        int i;
//        Melder_assert(n >= 0 && x > 0);
//        besselK_min2 = NUMbessel_k0_f(x);
//        if (n == 0) return besselK_min2;
//        besselK_min1 = NUMbessel_k1_f(x);
//        if (n == 1) return besselK_min1;
//        Melder_assert(n >= 2);
//        twoByX = 2.0 / x;
//        /*
//            Recursion formula.
//        */
//        for (i = 1; i < n; i++)
//        {
//            besselK = besselK_min2 + twoByX * i * besselK_min1;
//            besselK_min2 = besselK_min1;
//            besselK_min1 = besselK;
//        }
//        Melder_assert(NUMdefined(besselK));
//        return besselK;
//    }
//
//    double NUMbesselK(int n, double x)
//    {
//        gsl_sf_result result;
//        int status = gsl_sf_bessel_Kn_e(n, x, & result);
//        return status == GSL_SUCCESS ? result.val : NUMundefined;
//    }
//
//    double NUMsigmoid(double x)
//    { return x > 0.0 ? 1 / (1 + Math.exp(-x)) : 1 - 1 / (1 + Math.exp(x)); }
//
//    double NUMinvSigmoid(double x)
//    { return x <= 0.0 || x >= 1.0 ? NUMundefined : Math.log(x / (1.0 - x)); }
//
//    double NUMerfcc(double x)
//    {
//        gsl_sf_result result;
//        int status = gsl_sf_erfc_e(x, & result);
//        return status == GSL_SUCCESS ? result.val : NUMundefined;
//    }
//
//    double NUMgaussP(double z)
//    {
//        return 1 - 0.5 * NUMerfcc(NUMsqrt1_2 * z);
//    }
//
//    double NUMgaussQ(double z)
//    {
//        return 0.5 * NUMerfcc(NUMsqrt1_2 * z);
//    }
//
//    double NUMincompleteGammaP(double a, double x)
//    {
//        gsl_sf_result result;
//        int status = gsl_sf_gamma_inc_P_e(a, x, & result);
//        return status == GSL_SUCCESS ? result.val : NUMundefined;
//    }
//
//    double NUMincompleteGammaQ(double a, double x)
//    {
//        gsl_sf_result result;
//        int status = gsl_sf_gamma_inc_Q_e(a, x, & result);
//        return status == GSL_SUCCESS ? result.val : NUMundefined;
//    }
//
//    double NUMchiSquareP(double chiSquare, double degreesOfFreedom)
//    {
//        if (chiSquare < 0 || degreesOfFreedom <= 0) return NUMundefined;
//        return NUMincompleteGammaP(0.5 * degreesOfFreedom, 0.5 * chiSquare);
//    }
//
//    double NUMchiSquareQ(double chiSquare, double degreesOfFreedom)
//    {
//        if (chiSquare < 0 || degreesOfFreedom <= 0) return NUMundefined;
//        return NUMincompleteGammaQ(0.5 * degreesOfFreedom, 0.5 * chiSquare);
//    }
//
//    double NUMcombinations(int n, int k)
//    {
//        double result = 1.0;
//        int i;
//        if (k > n / 2) k = n - k;
//        for (i = 1; i <= k; i++) result *= n - i + 1;
//        for (i = 2; i <= k; i++) result /= i;
//        return result;
//    }
//
//    #
//    define NUM_interpolate_simple_cases
//    \
//            if(nx<1)return NUMundefined;\
//        if(x>nx)return y[nx];\
//        if(x<1)return y[1];\
//        if(x==midleft)return y[midleft];\
//        /* 1 < x < nx && x not integer: interpolate. */ \
//        if(maxDepth>midright-1)maxDepth=midright-1;\
//        if(maxDepth>nx-midleft)maxDepth=nx-midleft;\
//        if(maxDepth<=NUM_VALUE_INTERPOLATE_NEAREST)return y[(int)
//
//    floor(x+0.5)
//
//    ];\
//        if(maxDepth==NUM_VALUE_INTERPOLATE_LINEAR)return y[midleft]+(x-midleft)*(y[midright]-y[midleft]);\
//        if(maxDepth==NUM_VALUE_INTERPOLATE_CUBIC)
//
//    {\
//        double yl = y[midleft], yr = y[midright];\
//        double dyl = 0.5 * (yr - y[midleft - 1]), dyr = 0.5 * (y[midright + 1] - yl);\
//        double fil = x - midleft, fir = midright - x;\
//        return yl * fir + yr * fil - fil * fir * (0.5 * (dyr - dyl) + (fil - 0.5) * (dyl + dyr - 2 * (yr - yl)));\
//    }
//
//    #if
//
//    defined(__POWERPC__)
//
//    double NUM_interpolate_sinc(double y[], int nx, double x, int maxDepth)
//    {
//        int ix, midleft = floor(x), midright = midleft + 1, left, right;
//        double result = 0.0, a, halfsina, aa, daa, cosaa, sinaa, cosdaa, sindaa;
//        NUM_interpolate_simple_cases
//                left = midright - maxDepth, right = midleft + maxDepth;
//        a = NUMpi * (x - midleft);
//        halfsina = 0.5 * sin(a);
//        aa = a / (x - left + 1);
//        cosaa = Math.cos(aa);
//        sinaa = sin(aa);
//        daa = NUMpi / (x - left + 1);
//        cosdaa = Math.cos(daa);
//        sindaa = sin(daa);
//        for (ix = midleft; ix >= left; ix--)
//        {
//            double d = halfsina / a * (1.0 + cosaa), help;
//            result += y[ix] * d;
//            a += NUMpi;
//            help = cosaa * cosdaa - sinaa * sindaa;
//            sinaa = cosaa * sindaa + sinaa * cosdaa;
//            cosaa = help;
//            halfsina = -halfsina;
//        }
//        a = NUMpi * (midright - x);
//        halfsina = 0.5 * sin(a);
//        aa = a / (right - x + 1);
//        cosaa = Math.cos(aa);
//        sinaa = sin(aa);
//        daa = NUMpi / (right - x + 1);
//        cosdaa = Math.cos(daa);
//        sindaa = sin(daa);
//        for (ix = midright; ix <= right; ix++)
//        {
//            double d = halfsina / a * (1.0 + cosaa), help;
//            result += y[ix] * d;
//            a += NUMpi;
//            help = cosaa * cosdaa - sinaa * sindaa;
//            sinaa = cosaa * sindaa + sinaa * cosdaa;
//            cosaa = help;
//            halfsina = -halfsina;
//        }
//        return result;
//    }
//
//    #else
//
//    double NUM_interpolate_sinc(double y[], int nx, double x, int maxDepth)
//    {
//        int ix, midleft = floor(x), midright = midleft + 1, left, right;
//        double result = 0.0, a, halfsina, aa, daa;
//        NUM_interpolate_simple_cases
//                left = midright - maxDepth, right = midleft + maxDepth;
//        a = NUMpi * (x - midleft);
//        halfsina = 0.5 * sin(a);
//        aa = a / (x - left + 1);
//        daa = NUMpi / (x - left + 1);
//        for (ix = midleft; ix >= left; ix--)
//        {
//            double d = halfsina / a * (1.0 + Math.cos(aa));
//            result += y[ix] * d;
//            a += NUMpi;
//            aa += daa;
//            halfsina = -halfsina;
//        }
//        a = NUMpi * (midright - x);
//        halfsina = 0.5 * sin(a);
//        aa = a / (right - x + 1);
//        daa = NUMpi / (right - x + 1);\
//        for (ix = midright; ix <= right; ix++)
//        {
//            double d = halfsina / a * (1.0 + Math.cos(aa));
//            result += y[ix] * d;
//            a += NUMpi;
//            aa += daa;
//            halfsina = -halfsina;
//        }
//        return result;
//    }
//
//    #endif
//
//    /********** Improving extrema **********/
//    #
//    pragma mark
//    Improving extrema
//
//    struct improve_params
//
//    {
//        int depth;
//        double y;
//        int ixmax;
//        int isMaximum;
//    }
//
//    ;
//
//    static double improve_evaluate(double x, void closure)
//    {
//        struct improve_params*me = (struct improve_params *)closure;
//        double y = NUM_interpolate_sinc(my y, my ixmax, x, my depth);
//        return my isMaximum ? -y : y;
//    }
//
//    double NUMimproveExtremum(double y, int nx, int ixmid, int interpolation, double ixmid_real, int isMaximum)
//    {
//        struct improve_params params;
//        double result;
//        if (ixmid <= 1)
//        {*ixmid_real = 1;
//            return y[1];
//        }
//        if (ixmid >= nx)
//        {*ixmid_real = nx;
//            return y[nx];
//        }
//        if (interpolation <= NUM_PEAK_INTERPOLATE_NONE)
//        {*ixmid_real = ixmid;
//            return y[ixmid];
//        }
//        if (interpolation == NUM_PEAK_INTERPOLATE_PARABOLIC)
//        {
//            double dy = 0.5 * (y[ixmid + 1] - y[ixmid - 1]);
//            double d2y = 2 * y[ixmid] - y[ixmid - 1] - y[ixmid + 1];
//            *ixmid_real = ixmid + dy / d2y;
//            return y[ixmid] + 0.5 * dy * dy / d2y;
//        }
//        /* Sinc interpolation. */
//        params.y = y;
//        params.depth = interpolation == NUM_PEAK_INTERPOLATE_SINC70 ? 70 : 700;
//        params.ixmax = nx;
//        params.isMaximum = isMaximum;
//        /*return isMaximum ?
//            - NUM_minimize (ixmid - 1, ixmid, ixmid + 1, improve_evaluate, & params, 1e-10, 1e-11, ixmid_real) :
//            NUM_minimize (ixmid - 1, ixmid, ixmid + 1, improve_evaluate, & params, 1e-10, 1e-11, ixmid_real);*/
//        *ixmid_real = NUMminimize_brent(improve_evaluate, ixmid - 1, ixmid + 1, & params, 1e-10,&
//        result);
//        return isMaximum ? -result : result;
//    }
//
//    double NUMimproveMaximum(double y, int nx, int ixmid, int interpolation, double ixmid_real)
//    { return NUMimproveExtremum(y, nx, ixmid, interpolation, ixmid_real, 1); }
//
//    double NUMimproveMinimum(double y, int nx, int ixmid, int interpolation, double ixmid_real)
//    { return NUMimproveExtremum(y, nx, ixmid, interpolation, ixmid_real, 0); }
//
//    /**
//     * ******* Viterbi *********
//     */
//
//    void NUM_viterbi(
//            int numberOfFrames, int maxnCandidates,
//            int(*getNumberOfCandidates)
//
//    (
//    int iframe, void closure),
//
//    double(*getLocalCost)
//
//    (
//    int iframe, int icand, void closure),
//
//    double(*getTransitionCost)
//
//    (
//    int iframe, int icand1, int icand2, void closure),
//
//    void(*putResult)
//
//    (
//    int iframe, int place, void closure),
//    void closure)
//
//    {
//        autoNUMmatrix<double> delta (1, numberOfFrames, 1, maxnCandidates);
//        autoNUMmatrix<int> psi (1, numberOfFrames, 1, maxnCandidates);
//        autoNUMvector<int> numberOfCandidates (1, numberOfFrames);
//        for (int iframe = 1; iframe <= numberOfFrames; iframe++)
//        {
//            numberOfCandidates[iframe] = getNumberOfCandidates(iframe, closure);
//            for (int icand = 1; icand <= numberOfCandidates[iframe]; icand++)
//                delta[iframe][icand] = -getLocalCost(iframe, icand, closure);
//        }
//        for (int iframe = 2; iframe <= numberOfFrames; iframe++)
//        {
//            for (int icand2 = 1; icand2 <= numberOfCandidates[iframe]; icand2++)
//            {
//                double maximum = -1e300;
//                double place = 0;
//                for (int icand1 = 1; icand1 <= numberOfCandidates[iframe - 1]; icand1++)
//                {
//                    double value = delta[iframe - 1][icand1] + delta[iframe][icand2]
//                            - getTransitionCost(iframe, icand1, icand2, closure);
//                    if (value > maximum)
//                    {
//                        maximum = value;
//                        place = icand1;
//                    }
//                }
//                if (place == 0)
//                    Melder_throw("Viterbi algorithm cannot compute a track because of weird values.");
//                delta[iframe][icand2] = maximum;
//                psi[iframe][icand2] = place;
//            }
//        }
//        /*
//         * Find the end of the most probable path.
//         */
//        int place;
//        double maximum = delta[numberOfFrames][place = 1];
//        for (int icand = 2; icand <= numberOfCandidates[numberOfFrames]; icand++)
//            if (delta[numberOfFrames][icand] > maximum)
//                maximum = delta[numberOfFrames][place = icand];
//        /*
//         * Backtrack.
//         */
//        for (int iframe = numberOfFrames; iframe >= 1; iframe--)
//        {
//            putResult(iframe, place, closure);
//            place = psi[iframe][place];
//        }
//    }
//
//    /**
//     * **************
//     */
//
//    struct parm2
//
//    {
//        int ntrack;
//        int ncomb;
//        int**indices;
//        double( * getLocalCost)( int iframe, int icand, int itrack,void closure);
//        double( * getTransitionCost)( int iframe, int icand1, int icand2, int itrack,void
//        closure);
//        void( * putResult)( int iframe, int place, int itrack,void closure);
//        void closure;
//    }
//
//    ;
//
//    static int getNumberOfCandidates_n(int iframe, void closure)
//    {
//        struct parm2*me = (struct parm2 *)closure;
//        (void) iframe;
//        return my ncomb;
//    }
//
//    static double getLocalCost_n(int iframe, int jcand, void closure)
//    {
//        struct parm2*me = (struct parm2 *)closure;
//        double localCost = 0.0;
//        for (int itrack = 1; itrack <= my ntrack;
//        itrack++)
//        localCost += my getLocalCost(iframe, my indices[jcand][itrack], itrack, my closure);
//        return localCost;
//    }
//
//    static double getTransitionCost_n(int iframe, int jcand1, int jcand2, void closure)
//    {
//        struct parm2*me = (struct parm2 *)closure;
//        double transitionCost = 0.0;
//        for (int itrack = 1; itrack <= my ntrack;
//        itrack++)
//        transitionCost += my getTransitionCost(iframe,
//                                               my indices[jcand1][itrack], my indices[jcand2][itrack], itrack, my closure);
//        return transitionCost;
//    }
//
//    static void putResult_n(int iframe, int jplace, void closure)
//    {
//        struct parm2*me = (struct parm2 *)closure;
//        for (int itrack = 1; itrack <= my ntrack;
//        itrack++)
//        my putResult (iframe, my indices[jplace][itrack], itrack, my closure);
//    }
//
//    void NUM_viterbi_multi(
//            int nframe, int ncand, int ntrack,
//            double(*getLocalCost)
//
//    (
//    int iframe, int icand, int itrack, void closure),
//
//    double(*getTransitionCost)
//
//    (
//    int iframe, int icand1, int icand2, int itrack, void closure),
//
//    void(*putResult)
//
//    (
//    int iframe, int place, int itrack, void closure),
//    void closure)
//
//    {
//        struct parm2 parm;
//        parm.indices = NULL;
//
//        if (ntrack > ncand) Melder_throw("(NUM_viterbi_multi:) "
//                                         "Number of tracks (", ntrack, ") should not exceed number of candidates (", ncand, ").");
//        double ncomb = NUMcombinations(ncand, ntrack);
//        if (ncomb > 10000000) Melder_throw("(NUM_viterbi_multi:) "
//                                           "Unrealistically high number of combinations (", ncomb, ").");
//        parm.ntrack = ntrack;
//        parm.ncomb = ncomb;
//
//        /*
//         * For ncand == 5 and ntrack == 3, parm.indices is going to contain:
//         *   1 2 3
//         *   1 2 4
//         *   1 2 5
//         *   1 3 4
//         *   1 3 5
//         *   1 4 5
//         *   2 3 4
//         *   2 3 5
//         *   2 4 5
//         *   3 4 5
//         */
//        autoNUMmatrix<int> indices (1, parm.ncomb, 1, ntrack);
//        parm.indices = indices.peek();
//        autoNUMvector<int> icand (1, ntrack);
//        for (int itrack = 1; itrack <= ntrack; itrack++)
//            icand[itrack] = itrack;   // start out with "1 2 3"
//        int jcomb = 0;
//        for (; ; )
//        {
//            jcomb++;
//            for (int itrack = 1; itrack <= ntrack; itrack++)
//                parm.indices[jcomb][itrack] = icand[itrack];
//            int itrack = ntrack;
//            for (; itrack >= 1; itrack--)
//            {
//                if (++icand[itrack] <= ncand - (ntrack - itrack))
//                {
//                    for (int jtrack = itrack + 1; jtrack <= ntrack; jtrack++)
//                        icand[jtrack] = icand[itrack] + jtrack - itrack;
//                    break;
//                }
//            }
//            if (itrack == 0) break;
//        }
//        Melder_assert(jcomb == ncomb);
//        parm.getLocalCost = getLocalCost;
//        parm.getTransitionCost = getTransitionCost;
//        parm.putResult = putResult;
//        parm.closure = closure;
//        NUM_viterbi(nframe, ncomb, getNumberOfCandidates_n, getLocalCost_n, getTransitionCost_n, putResult_n, & parm)
//        ;
//    }
//
//    int NUMrotationsPointInPolygon(double x0, double y0, int n, double x[], double y[])
//    {
//        int nup = 0, i;
//        int upold = y[n] > y0, upnew;
//        for (i = 1; i <= n; i++)
//            if ((upnew = y[i] > y0) != upold)
//            {
//                int j = i == 1 ? n : i - 1;
//                if (x0 < x[i] + (x[j] - x[i]) * (y0 - y[i]) / (y[j] - y[i]))
//                {
//                    if (upnew) nup++;
//                    else nup--;
//                }
//                upold = upnew;
//            }
//        return nup;
//    }

}