import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import JobsPage from "./JobsPage.jsx";

export default function LandingPage() {
  const { user } = useAuth();
  return (
    <>
      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow"><span className="eyebrow-dot" /> YOUR NEXT CHAPTER STARTS HERE</span>
          <h1>Great careers.<br /><span>Start on campus.</span></h1>
          <p>One place to discover opportunities, connect with employers, and keep your placement journey moving.</p>
          <div className="hero-actions">
            <a className="button button-primary" href="#opportunities">Explore opportunities <span aria-hidden="true">→</span></a>
            <Link className="button button-outline" to={user ? "/dashboard" : "/register"}>{user ? "Open my dashboard" : "Create your profile"}</Link>
          </div>
          <div className="hero-proof"><div className="proof-avatars"><span>A</span><span>M</span><span>S</span></div><span>One campus community. Many possibilities.</span></div>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
          <div className="art-card art-card-main"><div className="art-card-icon">✦</div><span>YOUR CAREER JOURNEY</span><strong>Opportunity<br />looks good on you.</strong><div className="art-chart"><i /><i /><i /><i /><i /><i /><i /><i /><i /></div></div>
          <div className="art-card art-card-float"><span className="float-check">✓</span><span><b>Profile ready</b><small>You're set to get noticed</small></span></div>
          <div className="art-sparkle sparkle-a">✳</div><div className="art-sparkle sparkle-b">✦</div>
        </div>
      </section>
      <section className="trust-strip">
        <div><strong>01</strong><span>Discover roles<br />that fit your goals</span></div>
        <div><strong>02</strong><span>Connect with<br />hiring teams</span></div>
        <div><strong>03</strong><span>Track every step<br />of your journey</span></div>
      </section>
      <div id="opportunities"><JobsPage embedded /></div>
    </>
  );
}
