package me.kverna.hjornet.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

@Entity @Table(name = "AITEM")
@Data @AllArgsConstructor @NoArgsConstructor
public class Item implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String title;
    String description;
    long price;

    @ManyToOne(fetch = FetchType.LAZY)
    User owner;

    @JsonbTransient
    public void setOwner(User owner) {
        this.owner = owner;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    User buyer;

    @JsonbTransient
    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }

    @Version
    Timestamp version;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date created;

    @PrePersist
    protected void onCreate() {
        created = new Date();
    }
}
