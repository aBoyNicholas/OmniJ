package foundation.omni.consensus

import foundation.omni.CurrencyID
import foundation.omni.netapi.omniwallet.OmniwalletAbstractClient
import foundation.omni.rest.omniwallet.mjdk.OmniwalletModernJDKClient
import foundation.omni.rpc.ConsensusFetcher
import foundation.omni.rpc.ConsensusSnapshot
import foundation.omni.rpc.SmartPropertyListInfo
import groovy.transform.TypeChecked
import org.bitcoinj.params.MainNetParams

/**
 * Consensus Fetcher using Omniwallet REST API
 * @deprecated Use one of the implementations of {@link OmniwalletAbstractClient}
 */
@Deprecated
abstract class OmniwalletConsensusFetcher implements ConsensusFetcher {
    static URI OmniHost_Live = OmniwalletAbstractClient.omniwalletBase;

    private final OmniwalletAbstractClient client;
    
    @TypeChecked
    OmniwalletConsensusFetcher () {
        this(OmniHost_Live);
    }

    @TypeChecked
    OmniwalletConsensusFetcher (URI hostURI) {
        client = new OmniwalletModernJDKClient(hostURI, false, true, MainNetParams.get())
    }

    @Override
    @TypeChecked
    ConsensusSnapshot getConsensusSnapshot(CurrencyID currencyID) {
        return client.getConsensusSnapshot(currencyID)
    }
}
