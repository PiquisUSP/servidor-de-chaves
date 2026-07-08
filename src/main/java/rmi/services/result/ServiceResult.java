package rmi.services.result;

import java.io.Serializable;

// Serializable: é retornado por RMI (consultarChave), então precisa ser
// marshallável para trafegar do servidor até a instituição.
public class ServiceResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public int statusCode;

    public ServiceResult(int statusCode) {
        this.statusCode = statusCode;
    }
}