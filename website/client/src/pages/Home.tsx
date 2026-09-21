  return <div className="matrix-site">
    <TopDock repositoryUrl={REPOSITORY_URL} />
    <main id="top" ref={mainRef}><div className="matrix-viewport"><div className="matrix-camera" ref={cameraRef}><MatrixField fieldRef={matrixFieldRef} gridRef={matrixGridRef} /></div></div>
      <section className="matrix-scene hero-scene" ref={heroRef} aria-labelledby="hero-title">
        <div className="status-banner" aria-label="Keep Android Open">
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
            <path d="M12 2L2 7l10 5 10-5-10-5Zm-8 8 8 4 8-4v7l-8 4-8-4v-7Z" fill="currentColor" />
          </svg>
          <p>
            <strong>KEEP ANDROID OPEN</strong> — Android should stay open, flexible, and user-controlled.
          </p>
          <span className="status-banner-cta">android freedom</span>
        </div>

        <div className="hero-content gsap-reveal">
          <h1 id="hero-title">music in<br /><strong>motion.</strong></h1>
          <p>Glyphix turns your Nothing Glyph Interface into a responsive light show for the music you already love—tuned for clarity, rhythm, and a more personal device experience.</p>
          <div className="hero-actions">
            <a className="action-button action-primary" href={REPOSITORY_URL} target="_blank" rel="noreferrer">Open GitHub <ArrowDownRight size={17} /></a>
            <a className="action-button action-quiet" href="#mapping">See the mapping <span aria-hidden="true" /></a>
          </div>
        </div>
      </section>
      <section className="matrix-scene mapping-scene" id="mapping" aria-labelledby="mapping-title"><div className="mapping-copy gsap-reveal"><h2 id="mapping-title">Each <em>light</em>,<br />gets its own <em>voice.</em></h2><p>Glyphix analyzes the shape of your audio and converts it into the Glyph display design language—tight and readable, not just decorative.</p></div><div className={`zone-card gsap-reveal ${zonesVisible ? "is-visible" : ""}`} ref={zoneCardRef}><div className="zone-card-head"><span>Glyph bands</span><b>live</b></div>{zones.map(([id, label, range], index) => <div key={id} className="zone-line" style={{ "--delay": `${index * 90}ms`, "--meter": `${30 + index * 11}%` } as CSSProperties}><span>{id}</span><strong>{label}</strong><i><b /></i><em>{range}</em></div>)}</div></section>
      <section className="matrix-scene brightness-scene" id="brightness" aria-labelledby="brightness-title"><div className="brightness-copy gsap-reveal"><h2 id="brightness-title">More than<br />just a <em>screen</em>.</h2><p>Glyphix keeps the lights responsive in real time, using subtle brightness changes to preserve the feel of the song instead of turning it into a noisy effect.</p><a className="action-button action-primary" href={REPOSITORY_URL} target="_blank" rel="noreferrer">Explore the repo <ArrowDownRight size={17} /></a></div><div className="brightness-minimal gsap-reveal"><div className="brightness-minimal-value"><span>response</span><strong>1.8k<span>Hz</span></strong></div><div className="brightness-minimal-bar"><i /></div><div className="brightness-minimal-facts"><span>gain <b>92<span>%</span></b></span><span>latency <b>12<span>ms</span></b></span></div></div></section>
      <section className="matrix-scene response-scene" id="response" aria-labelledby="response-title"><div className={`response-graphic gsap-reveal ${responseActive ? "is-audio-active" : "is-audio-idle"}`} style={responseStyle}><span className="response-ring ring-a" /><span className="response-ring ring-b" /><span className="response-ring ring-c" /><span className="response-core"><Volume2 size={26} /></span></div><div className="response-copy gsap-reveal"><h2 id="response-title">Listen to the <em>pulse.</em></h2><p>When a track plays, Glyphix reacts in sync with the beat, creating a visible, physical rhythm without overpowering the device UI.</p><div className="response-notes"><span><i />bass</span><span><i />low-mid</span><span><i />air</span></div></div></section>
      <section className="matrix-scene devices-scene" id="devices" aria-labelledby="devices-title"><div className="devices-title gsap-reveal"><h2 id="devices-title">Supported<br /><em>phones.</em></h2><p>Glyphix is tuned for the Nothing phone family and keeps compatibility clear for the devices it supports best.</p></div><div className="device-groups gsap-reveal"><div className="device-list"><div className="device-list-head"><span>Supported</span><b>devices</b></div>{supportedDevices.map((device) => <div key={device} className="device-line"><span>01</span><strong>{device}</strong><i /></div>)}</div></div></section>
      <section className="matrix-scene finale-scene" aria-labelledby="finale-title">
        <div className="finale-matrix" ref={finaleRef} aria-hidden="true">
          <i className="finale-ring finale-ring-1" />
          <i className="finale-ring finale-ring-2" />
          <i className="finale-ring finale-ring-3" />
          <i className="finale-ring finale-ring-4" />
          <i className="finale-orbit finale-orbit-a"><b /></i>
          <i className="finale-orbit finale-orbit-b"><b /></i>
        </div>
        <div className="finale-content gsap-reveal"><h2 id="finale-title">Play a track.<br /><em>Watch the lights.</em></h2><p>Download Glyphix and try it with your own music.</p><div className="finale-actions"><a className="action-button action-primary" href={RELEASES_URL} target="_blank" rel="noreferrer">Download Glyphix <ArrowDownRight size={17} /></a><a className="action-button action-quiet" href="/support">Need help? <span aria-hidden="true" /></a></div></div>
      </section>
      <audio ref={deckARef} src={tracks[0].source} preload="auto" onPlay={beginPlayback} onError={handleAudioError} onTimeUpdate={handleTimeUpdate("a")} onLoadedMetadata={(event) => { if (activeDeckRef.current === "a") setDuration(event.currentTarget.duration || 0); }} />
      <audio ref={deckBRef} preload="auto" onPlay={beginPlayback} onError={handleAudioError} onTimeUpdate={handleTimeUpdate("b")} onLoadedMetadata={(event) => { if (activeDeckRef.current === "b") setDuration(event.currentTarget.duration || 0); }} />
    </main>
    <SiteFooter />
  </div>;
}