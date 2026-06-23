package com.example.sp.model.promotion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "khach_hang_pgg")
@Getter
@Setter
@NoArgsConstructor
public class KhachHangPgg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_kh")
    private Integer idKh;

    @Column(name = "id_pgg")
    private Integer idPgg;
}
