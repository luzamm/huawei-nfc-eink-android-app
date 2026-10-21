package org.nfcresearch.qpolllab;

/** Huawei JD79665AA panel variants; selection is physical, never inferred from D1. */
public enum PanelType {
    A0, A1, A0X;
    public static PanelType parse(String value) {
        if("A0".equals(value))return A0;
        if("A1".equals(value))return A1;
        if("A0X".equals(value))return A0X;
        throw new IllegalArgumentException("请先选择屏幕类型 A0、A1 或 A0临时");
    }
    public byte[] pack(byte[] codes) {
        byte[] packed=map(PanelProtocol.packCodes(codes,PanelProtocol.WIDTH,PanelProtocol.HEIGHT),false);
        return this==A0X?swapHalves(packed):packed;
    }
    /** Temporary physical variant: A0 layout with the two 276-column halves exchanged. */
    private static byte[] swapHalves(byte[] data) {
        if(data.length!=PanelProtocol.IMAGE_BYTES)throw new IllegalArgumentException("屏幕数据长度错误");
        int half=PanelProtocol.WIDTH/2*(PanelProtocol.HEIGHT/4);
        byte[] out=new byte[data.length];
        System.arraycopy(data,half,out,0,half);
        System.arraycopy(data,0,out,half,half);
        return out;
    }
    private byte[] map(byte[] raw,boolean inverse) {
        if(raw.length!=PanelProtocol.IMAGE_BYTES)throw new IllegalArgumentException("屏幕数据长度错误");
        if(this==A1)return raw.clone();
        byte[] out=new byte[raw.length];
        // Transposed controller rows: first half -> even rows, second half -> reversed odd rows.
        final int rows=PanelProtocol.WIDTH,stride=PanelProtocol.HEIGHT/4;
        for(int row=0;row<rows;row++) {
            int target=row<rows/2?2*row:2*rows-1-2*row;
            System.arraycopy(raw,(inverse?target:row)*stride,out,(inverse?row:target)*stride,stride);
        }
        return out;
    }
    public int[] preview(byte[] packed) {
        byte[] data=this==A0X?swapHalves(packed):packed;
        return ImageRaster.preview(map(data,true),PanelProtocol.WIDTH,PanelProtocol.HEIGHT);
    }
    /** Stored scan 00 uses bottom-up pixels; scan 01 uses top-down pixels in each row. */
    public static byte[] forScan(byte[] packed,boolean topDown) {
        if(packed.length!=PanelProtocol.IMAGE_BYTES)throw new IllegalArgumentException("屏幕数据长度错误");
        if(!topDown)return packed.clone();
        int stride=PanelProtocol.HEIGHT/4;byte[] out=new byte[packed.length];
        for(int row=0;row<PanelProtocol.WIDTH;row++)for(int i=0;i<stride;i++) {
            int b=packed[row*stride+stride-1-i]&255;
            out[row*stride+i]=(byte)(((b&3)<<6)|((b&12)<<2)|((b&48)>>2)|((b&192)>>6));
        }
        return out;
    }
}
