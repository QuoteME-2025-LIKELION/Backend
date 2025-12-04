@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class QuoteTagRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_member_id")
    private Member requester; // 태그 요청한 사람

    @CreatedDate
    private LocalDateTime createDate;

    @Builder
    public QuoteTagRequest(Quote quote, Member requester) {
        this.quote = quote;
        this.requester = requester;
    }
}