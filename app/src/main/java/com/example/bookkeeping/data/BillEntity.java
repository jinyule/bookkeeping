package com.example.bookkeeping.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "bills",
        indices = {
                @Index(value = {"fingerprint"}, unique = true),
                @Index(value = {"transactionTime"}),
                @Index(value = {"direction"}),
                @Index(value = {"sourceChannel"})
        }
)
public class BillEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long amountCents;

    @NonNull
    public String direction = "EXPENSE";

    @NonNull
    public String categoryKey = "other";

    public String merchant = "";

    public String account = "";

    public String note = "";

    public long transactionTime;

    public long createdAt;

    public long updatedAt;

    @NonNull
    public String sourceChannel = "MANUAL";

    public String rawRecognizedText = "";

    public double confidence;

    @NonNull
    public String reviewStatus = "ACCEPTED";

    @NonNull
    public String fingerprint = "";
}
