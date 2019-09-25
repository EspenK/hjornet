package me.kverna.hjornet.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Base64;
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

    @OneToOne
    Image image;

    public void setImage(String encodedImage) throws IOException {
        Image image = new Image();

        String[] parts = encodedImage.split(";");
        String encodedString = parts[1].split(",")[1];
        String path = "img/" + image.getId() + DigestUtils.md5Hex(encodedString);

        byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
        FileUtils.writeByteArrayToFile(new File(path), decodedBytes);

        image.setType(parts[0].split(":")[1]);
        image.setPath(path);
        this.image = image;
    }

    @JsonbTransient
    public Image getImage() {
        return image;
    }

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
