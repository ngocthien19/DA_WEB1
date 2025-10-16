package vn.iotstar.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.iotstar.entity.VanChuyen;
import vn.iotstar.entity.DatHang;
import vn.iotstar.entity.CuaHang;
import vn.iotstar.entity.NguoiDung;

import java.util.List;

public interface VanChuyenService {
    
    // Đơn hàng chưa được gán shipper
    Page<DatHang> getUnassignedConfirmedOrdersByCuaHang(CuaHang cuaHang, String keyword, Pageable pageable);
    
    // Đơn hàng đã được gán shipper
    Page<VanChuyen> getAssignedOrdersByCuaHang(CuaHang cuaHang, String keyword, Pageable pageable);
    
    List<NguoiDung> getAllActiveShippers();
    
    long countUnassignedConfirmedOrdersByCuaHang(CuaHang cuaHang);
    
    long countAssignedOrdersByCuaHang(CuaHang cuaHang);
    
    VanChuyen assignShipperToOrder(Integer orderId, Integer shipperId);
    
    boolean isOrderAssigned(Integer orderId);
    
    VanChuyen getDeliveryByOrderId(Integer orderId);
    
    void unassignShipper(Integer deliveryId);
    
    VanChuyen getDeliveryById(Integer deliveryId);
    
    Page<VanChuyen> getDeliveriesByShipperAndStatus(Integer shipperId, String status, Pageable pageable);
    long countDeliveriesByShipperAndStatus(Integer shipperId, String status);
    List<VanChuyen> getDeliveriesByShipperId(Integer shipperId);
    
    VanChuyen updateDeliveryStatus(Integer deliveryId, String newStatus, String lyDoHuy);
    
    VanChuyen processDeliveryCancellation(Integer deliveryId, String lyDoHuy, String action);
}