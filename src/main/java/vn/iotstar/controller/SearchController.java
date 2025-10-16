	package vn.iotstar.controller;
	
	import java.io.UnsupportedEncodingException;
	import java.net.URLDecoder;
	import java.nio.charset.StandardCharsets;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	import java.util.stream.Collectors;
	
	import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
	import org.springframework.web.bind.annotation.GetMapping;
	import org.springframework.web.bind.annotation.RequestParam;
	import org.springframework.web.bind.annotation.ResponseBody;
	
	import vn.iotstar.entity.SanPham;
import vn.iotstar.repository.SanPhamRepository;
import vn.iotstar.service.SanPhamService;
import vn.iotstar.specification.SanPhamSpecification;
	
	@Controller
	public class SearchController {
	    
	    @Autowired
	    private SanPhamService sanPhamService;
	    
	    @Autowired
	    private SanPhamRepository sanPhamRepository;
	    
	    @GetMapping("/search")
	    public String search(@RequestParam(required = false) String search) {
	        if (search != null && !search.trim().isEmpty()) {
	            try {
	                String decoded = URLDecoder.decode(search, StandardCharsets.UTF_8.toString());
	                decoded = decoded.trim();
	                return "redirect:/products?search=" + java.net.URLEncoder.encode(decoded, StandardCharsets.UTF_8.toString());
	            } catch (UnsupportedEncodingException e) {
	                return "redirect:/products?search=" + search;
	            }
	        }
	        return "redirect:/products";
	    }
	    
	    @GetMapping("/api/search-suggestions")
	    @ResponseBody
	    public Map<String, Object> getSearchSuggestions(@RequestParam String keyword) {
	        Map<String, Object> result = new HashMap<>();
	        
	        try {
	            if (keyword != null && keyword.trim().length() >= 1) {
	                keyword = keyword.trim();
	                
	                // Sử dụng Specification
	                Specification<SanPham> spec = SanPhamSpecification.hasTenSanPhamContaining(keyword);
	                Pageable pageable = PageRequest.of(0, 10);
	                List<SanPham> matchingProducts = sanPhamRepository.findAll(spec, pageable).getContent();
	                
	                List<Map<String, Object>> productList = matchingProducts.stream()
	                    .map(sp -> {
	                        Map<String, Object> product = new HashMap<>();
	                        product.put("maSanPham", sp.getMaSanPham());
	                        product.put("tenSanPham", sp.getTenSanPham() != null ? sp.getTenSanPham() : "");
	                        product.put("hinhAnh", sp.getHinhAnh() != null ? sp.getHinhAnh() : "/images/default-product.jpg");
	                        product.put("giaBan", sp.getGiaBan() != null ? sp.getGiaBan() : 0);
	                        return product;
	                    })
	                    .collect(Collectors.toList());
	                
	                List<Map<String, Object>> recommended = productList.stream()
	                    .limit(6)
	                    .collect(Collectors.toList());
	                
	                result.put("products", productList);
	                result.put("recommended", recommended);
	                result.put("success", true);
	            } else {
	                result.put("products", List.of());
	                result.put("recommended", List.of());
	                result.put("success", true);
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            result.put("products", List.of());
	            result.put("recommended", List.of());
	            result.put("success", false);
	            result.put("error", "Có lỗi xảy ra khi tìm kiếm");
	        }
	        
	        return result;
	    }
	}