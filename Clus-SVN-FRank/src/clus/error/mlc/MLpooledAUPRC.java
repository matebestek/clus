
package clus.error.mlc;

import java.io.PrintWriter;
import java.text.NumberFormat;

import clus.data.type.primitive.NominalAttrType;
import clus.error.common.ClusError;
import clus.error.common.ClusErrorList;
import clus.main.settings.Settings;
import clus.util.ClusFormat;


public class MLpooledAUPRC extends MLROCAndPRCurve {
 
    private static final long serialVersionUID = Settings.SERIAL_VERSION_ID;
    private final int m_Measure = pooledAUPRC;


    public MLpooledAUPRC(ClusErrorList par, NominalAttrType[] nom) {
        super(par, nom);
    }


    public double getModelError() {
        return getModelError(m_Measure);
    }


    public String getName() {
        return "pooledAUPRC";
    }


    public void showModelError(PrintWriter out, int detail) {
        NumberFormat fr1 = ClusFormat.SIX_AFTER_DOT;
        computeAll();
        out.println(fr1.format(m_PooledAUPRC));
    }


    public ClusError getErrorClone(ClusErrorList par) {
        return new MLpooledAUPRC(par, m_Attrs);
    }
}
