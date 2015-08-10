package foundation.omni.test.rpc.sendall

import foundation.omni.BaseRegTestSpec
import foundation.omni.CurrencyID
import foundation.omni.Ecosystem
import foundation.omni.PropertyType
import org.junit.internal.AssumptionViolatedException
import spock.lang.Unroll

class SendAllSpec extends BaseRegTestSpec {

    final static BigDecimal startBTC = 0.1
    final static BigDecimal startMSC = 0.1
    final static BigDecimal zeroAmount = 0.0

    @Unroll
    def "In #ecosystem all available tokens can be transferred with transaction type 4"() {
        when:
        def actorAddress = createFundedAddress(startBTC, startMSC)
        def otherAddress = newAddress

        then:
        getbalance_MP(actorAddress, CurrencyID.MSC).balance == startMSC
        getbalance_MP(actorAddress, CurrencyID.TMSC).balance == startMSC
        getbalance_MP(otherAddress, CurrencyID.MSC).balance == zeroAmount
        getbalance_MP(otherAddress, CurrencyID.TMSC).balance == zeroAmount

        when:
        def sendTxid = sendAll(actorAddress, otherAddress, ecosystem)
        generateBlock()
        def sendTx = getTransactionMP(sendTxid)

        then: "the transaction is valid"
        sendTx.valid

        and: "it has the specified values"
        sendTx.txid == sendTxid.toString()
        sendTx.sendingaddress == actorAddress.toString()
        sendTx.referenceaddress == otherAddress.toString()
        sendTx.type_int == 4
        sendTx.ecosystem == ecosystemToString(ecosystem)
        sendTx.containsKey('subsends')

        and:
        List<Map<String, Object>> subSends = sendTx['subsends']
        subSends.size() == 1
        subSends[0].propertyid == ecosystem.longValue()
        subSends[0].divisible
        subSends[0].amount as BigDecimal == startMSC

        and:
        if (ecosystem == Ecosystem.MSC) {
            assert getbalance_MP(actorAddress, CurrencyID.MSC).balance == zeroAmount
            assert getbalance_MP(actorAddress, CurrencyID.TMSC).balance == startMSC
            assert getbalance_MP(otherAddress, CurrencyID.MSC).balance == startMSC
            assert getbalance_MP(otherAddress, CurrencyID.TMSC).balance == zeroAmount
        } else {
            assert getbalance_MP(actorAddress, CurrencyID.MSC).balance == startMSC
            assert getbalance_MP(actorAddress, CurrencyID.TMSC).balance == zeroAmount
            assert getbalance_MP(otherAddress, CurrencyID.MSC).balance == zeroAmount
            assert getbalance_MP(otherAddress, CurrencyID.TMSC).balance == startMSC
        }

        where:
        ecosystem << [Ecosystem.MSC, Ecosystem.TMSC]
    }

    @Unroll
    def "In #ecosystem sending all tokens is only valid, if at least one unit was transferred"() {
        when:
        def actorAddress = createFundedAddress(startBTC, startMSC)
        def otherAddress = newAddress
        send_MP(actorAddress, otherAddress, CurrencyID.MSC, startMSC)
        send_MP(actorAddress, otherAddress, CurrencyID.TMSC, startMSC)
        generateBlock()

        then:
        getbalance_MP(actorAddress, CurrencyID.MSC).balance == zeroAmount
        getbalance_MP(actorAddress, CurrencyID.TMSC).balance == zeroAmount
        getbalance_MP(otherAddress, CurrencyID.MSC).balance == startMSC
        getbalance_MP(otherAddress, CurrencyID.TMSC).balance == startMSC

        when:
        def sendTxid = sendAll(actorAddress, otherAddress, ecosystem)
        generateBlock()

        then:
        getTransactionMP(sendTxid).valid == false

        and:
        getbalance_MP(actorAddress, CurrencyID.MSC).balance == zeroAmount
        getbalance_MP(actorAddress, CurrencyID.TMSC).balance == zeroAmount
        getbalance_MP(otherAddress, CurrencyID.MSC).balance == startMSC
        getbalance_MP(otherAddress, CurrencyID.TMSC).balance == startMSC

        where:
        ecosystem << [Ecosystem.MSC, Ecosystem.TMSC]
    }

    @Unroll
    def "In #ecosystem only available, unreserved balances are transferred, when sending all tokens"() {
        when:
        def actorAddress = createFundedAddress(startBTC, zeroAmount)
        def otherAddress = createFundedAddress(startBTC, zeroAmount)
        def nonManagedID = fundNewProperty(actorAddress, 10.0, PropertyType.DIVISIBLE, ecosystem)
        def tradeCurrency = new CurrencyID(ecosystem.longValue())

        then:
        getbalance_MP(actorAddress, nonManagedID).balance == 10.0
        getbalance_MP(otherAddress, nonManagedID).balance == zeroAmount

        when:
        def tradeTxid = trade_MP(actorAddress, nonManagedID, 4.0, tradeCurrency, 4.0, 1 as Byte)
        generateBlock()
        def tradeTx = getTransactionMP(tradeTxid)

        then:
        tradeTx.valid
        getbalance_MP(actorAddress, nonManagedID).balance == 6.0
        getbalance_MP(actorAddress, nonManagedID).reserved == 4.0

        when:
        def sendTxid = sendAll(actorAddress, otherAddress, ecosystem)
        generateBlock()
        def sendTx = getTransactionMP(sendTxid)

        then:
        sendTx.valid
        getbalance_MP(actorAddress, nonManagedID).balance == zeroAmount
        getbalance_MP(actorAddress, nonManagedID).reserved == 4.0
        getbalance_MP(otherAddress, nonManagedID).balance == 6.0

        where:
        ecosystem << [Ecosystem.MSC, Ecosystem.TMSC]
    }

    def ecosystemToString(Ecosystem ecosystem) {
        if (ecosystem == Ecosystem.MSC) {
            return "main"
        } else {
            return "test"
        }
    }

    def setupSpec() {
        if (!commandExists("omni_sendall")) {
            throw new AssumptionViolatedException('The client has no "omni_sendall" command')
        }
    }

}